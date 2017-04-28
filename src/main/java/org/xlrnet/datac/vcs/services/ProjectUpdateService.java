package org.xlrnet.datac.vcs.services;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.spring.events.EventBus;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.ProjectAlreadyInitializedException;
import org.xlrnet.datac.commons.graph.BreadthFirstTraverser;
import org.xlrnet.datac.commons.graph.DepthFirstTraverser;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.services.ChangeSetService;
import org.xlrnet.datac.database.services.LiquibaseProcessService;
import org.xlrnet.datac.foundation.EventTopics;
import org.xlrnet.datac.foundation.components.EventLogProxy;
import org.xlrnet.datac.foundation.domain.*;
import org.xlrnet.datac.foundation.services.*;
import org.xlrnet.datac.vcs.api.*;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;

/**
 * Service which is responsible for collecting all database changes in a project.
 */
@Service
@Transactional(timeout = 1800)      // FIXME: This is bad. We should separate transactional processing into separate controllers.
public class ProjectUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectUpdateService.class);

    /**
     * Application-wide event bus.
     */
    private final EventBus.ApplicationEventBus applicationEventBus;

    /**
     * The VCS Adapter that will be used for updating the project.
     */
    private final VersionControlSystemService vcsService;

    /**
     * Central locking service.
     */
    private final LockingService lockingService;

    /**
     * The file service for accessing file resources.
     */
    private final FileService fileService;

    /**
     * Project service for updating project data.
     */
    private final ProjectService projectService;

    /**
     * Service for updating branch data.
     */
    private final BranchService branchService;

    /**
     * Service for accessing the revision revision graph.
     */
    private final RevisionGraphService revisionGraphService;

    /**
     * Bean validation service.
     */
    private final ValidationService validator;

    /**
     * Event logging service.
     */
    private final EventLogService eventLogService;

    /**
     * Request-scoped event log proxy.
     */
    private final EventLogProxy eventLog;

    /**
     * Service for accessing and parsing changelog files.
     */
    private final LiquibaseProcessService liquibaseProcessService;

    /**
     * Service for accessing change sets.
     */
    private final ChangeSetService changeSetService;

    /**
     * Helper class for performing breadth first traversals on revision graphs.
     */
    private BreadthFirstTraverser<Revision> breadthFirstTraverser = new BreadthFirstTraverser<>();

    /**
     * Helper class for performing depth first traversals on revision graphs.
     */
    private DepthFirstTraverser<Revision> depthFirstTraverser = new DepthFirstTraverser<>();

    @Autowired
    public ProjectUpdateService(EventBus.ApplicationEventBus applicationEventBus, VersionControlSystemService vcsService, LockingService lockingService, FileService fileService, ProjectService projectService, BranchService branchService, RevisionGraphService revisionGraphService, ValidationService validator, EventLogService eventLogService, EventLogProxy eventLog, LiquibaseProcessService liquibaseProcessService, ChangeSetService changeSetService) {
        this.applicationEventBus = applicationEventBus;
        this.vcsService = vcsService;
        this.lockingService = lockingService;
        this.fileService = fileService;
        this.projectService = projectService;
        this.branchService = branchService;
        this.revisionGraphService = revisionGraphService;
        this.validator = validator;
        this.eventLogService = eventLogService;
        this.eventLog = eventLog;
        this.liquibaseProcessService = liquibaseProcessService;
        this.changeSetService = changeSetService;
    }

    /**
     * Asynchronously trigger a project change update.
     *
     * @param project
     *         The project to update.
     */
    @Async
    public void startAsynchronousProjectUpdate(@NotNull Project project) {
        startProjectUpdate(project);
    }

    private void startProjectUpdate(@NotNull Project project) {
        if (lockingService.tryLock(project)) {
            try {
                LOGGER.info("Begin update of project {}", project.getName());
                eventLog.setDelegate(eventLogService.newEventLog().setType(EventType.PROJECT_UPDATE));
                Project reloaded = projectService.findOne(project.getId());
                eventLog.setProject(reloaded);
                reloaded = updateProject(reloaded);
                if (reloaded.getState() != ProjectState.MISSING_LOG) {
                    LOGGER.info("Finished updating project {} [id={}] successfully", project.getName(), project.getId());
                    eventLog.addMessage(new EventLogMessage("Project update finished successfully"));
                }
            } catch (DatacTechnicalException e) {
                LOGGER.error("Update of project {} [id={}] failed because of an unexpected exception", project.getName(), project.getId(), e);
                projectService.markProjectAsFailedUpdate(project);
                eventLogService.addExceptionToEventLog(eventLog, "Project update failed because of an unexpected exception", e);
            } finally {
                try {
                    eventLogService.save(eventLog);
                } catch (DatacRuntimeException e) {
                    LOGGER.error("Writing eventlog after project update failed", e);
                } finally {
                    lockingService.unlock(project);
                }
            }
        } else {
            LOGGER.warn("Update of project {} [id={}] failed because project is locked", project.getName(), project.getId());
        }
    }

    /**
     * Internal main method for updating a project. If the project repository is not yet initialized, the VCS adapter
     * will be called to initialize a local repository.
     *
     * @param project
     *         The project to update.
     * @throws DatacTechnicalException
     *         Will be thrown if the project update failed.
     */
    @Transactional
    protected Project updateProject(@NotNull Project project) throws DatacTechnicalException {
        VcsAdapter vcsAdapter = vcsService.getVcsAdapter(project);
        project.setState(ProjectState.INITIALIZING);
        updateProjectState(project, 0);
        projectService.save(project);

        try {
            if (!project.isInitialized()) {
                initializeProjectRepository(project, vcsAdapter);
            }

            Path repositoryPath = fileService.getProjectRepositoryPath(project);

            LOGGER.debug("Opening local repository at {}", repositoryPath.toString());
            VcsLocalRepository localRepository = vcsAdapter.openLocalRepository(repositoryPath, project);

            Project updatedProject = updateRevisions(project, localRepository);
            updatedProject = indexDatabaseChanges(updatedProject, localRepository);

            updatedProject.setLastChangeCheck(LocalDateTime.now());
            if (updatedProject.getState() != ProjectState.MISSING_LOG) {
                updatedProject.setState(ProjectState.FINISHED);
            }
            updateProjectState(updatedProject, 0);
            return projectService.save(updatedProject);
        } catch (RuntimeException | IOException e) {
            throw new DatacTechnicalException("Project update failed", e);
        }
    }

    /**
     * Initialize a new project repository. This will first call the file service to create necessary file structures
     * and afterwards open a remote connection to the project's VCS to initialize local VCS files.
     *
     * @param project
     *         The project to update.
     * @param vcsAdapter
     *         The adapter to use for updating the project.
     * @throws DatacTechnicalException
     *         Will be thrown if the project update failed
     * @throws IOException
     *         Will be thrown on writing errors
     */
    protected void initializeProjectRepository(@NotNull Project project, @NotNull VcsAdapter vcsAdapter) throws DatacTechnicalException, IOException {
        LOGGER.info("Initializing new repository for project {}", project.getName());
        VcsRemoteRepositoryConnection vcsRemoteRepositoryConnection = vcsAdapter.connectRemote(project);
        VcsConnectionStatus vcsConnectionStatus = vcsRemoteRepositoryConnection.checkConnection();
        if (vcsConnectionStatus != VcsConnectionStatus.ESTABLISHED) {
            throw new DatacTechnicalException("Connection check failed. Status was " + vcsConnectionStatus);
        }

        Path repositoryPath;
        try {
            repositoryPath = fileService.prepareProjectRepositoryPath(project);
        } catch (ProjectAlreadyInitializedException pe) {   // NOSONAR: No logging necessary, since part of logic flow
            LOGGER.info("Repository for project {} [id={}] was already initialized - cleaning existing repository", project.getName(), project.getId());
            fileService.deleteProjectRepository(project);
            repositoryPath = fileService.prepareProjectRepositoryPath(project);
        }

        try {
            LOGGER.debug("Calling remote VCS adapter {} to initialize repository in {}", vcsRemoteRepositoryConnection.getClass().getName(), repositoryPath);
            vcsRemoteRepositoryConnection.initializeLocalRepository(repositoryPath, project.getDevelopmentBranch());

            project.setInitialized(true);
            projectService.save(project);
        } catch (DatacTechnicalException | RuntimeException e) {
            LOGGER.error("Initialization of project {} [id={}] failed - rolling back file system changes", e);
            eventLogService.addExceptionToEventLog(eventLog, "Initialization of local repository failed", e);
            try {
                fileService.deleteProjectRepository(project);
            } catch (DatacTechnicalException e2) {
                eventLogService.addExceptionToEventLog(eventLog, "Cleaning up file system during rollback failed", e);
                LOGGER.error("Critical error while cleaning up file system during rollback", e2);
            }
            throw e;
        }

        eventLog.addMessage(new EventLogMessage("Successfully initialized local repository"));
        LOGGER.info("Successfully initialized local repository for project {}", project.getName());
    }

    /**
     * Update the internal revision graph of the VCS. Checks for new branches and updates the revisions.
     *
     * @param project
     *         The project to update.
     * @param localRepository
     *         The local repository to interact with a VCS.
     */
    protected Project updateRevisions(@NotNull Project project, @NotNull VcsLocalRepository localRepository) throws DatacTechnicalException {
        LOGGER.debug("Checking for new branches in project {}", project.getName());
        Project updatedProject = projectService.updateAvailableBranches(project, localRepository);
        updatedProject.setState(ProjectState.UPDATING);
        updateProjectState(updatedProject, 0);
        updatedProject = projectService.save(updatedProject);

        LOGGER.debug("Updating revisions in project {}", updatedProject.getName());

        long branchCount = updatedProject.getBranches().stream().filter(b -> b.isWatched() || b.isDevelopment()).count();
        int updatedBranches = 0;
        for (Branch branch : updatedProject.getBranches()) {
            updateProjectState(updatedProject, updatedBranches / (double) branchCount * 100.f);
            if (branch.isWatched() || branch.isDevelopment()) {
                updateRevisionsInBranch(updatedProject, branch, localRepository);
                updatedBranches++;
            } else {
                LOGGER.debug("Skipping branch {} in project {}", branch.getName(), updatedProject.getName());
            }
        }
        LOGGER.debug("Finished revision update in project {}", updatedProject.getName());
        return updatedProject;
    }

    protected void updateRevisionsInBranch(@NotNull Project project, @NotNull Branch branch, @NotNull VcsLocalRepository localRepository) throws DatacTechnicalException {
        LOGGER.debug("Updating revisions on branch {} in project {}", branch.getName(), project.getName());
        localRepository.updateRevisionsFromRemote(branch);

        VcsRevision rootRevision = localRepository.listLatestRevisionOnBranch(branch);

        Pair<Revision, Long> latestRevision = convertRevision(rootRevision, project);
        LOGGER.trace("Saving revisions on branch {} in project {}", branch.getName(), project.getName());
        revisionGraphService.save(latestRevision.getLeft());
        LOGGER.debug("Finished updating revisions on branch {} in project {}", branch.getName(), project.getName());
        branch.setInternalId(latestRevision.getLeft().getInternalId());
        branchService.save(branch);
        if (latestRevision.getRight() != null && latestRevision.getRight() > 0) {
            eventLog.addMessage(new EventLogMessage(String.format("Found %d new revisions in branch %s", latestRevision.getRight(), branch.getName())));
        }
    }

    /**
     * Recursive implementation which converts external {@link VcsRevision} objects to {@link Revision} entities. If
     * any of the revision objects already exist in the database, the rest of the graph will be fetched from the
     * database.
     *
     * @param rootRevision
     *         The root revision used for starting the conversion.
     * @param project
     *         The project in which the revisions will be stored.
     */
    @NotNull
    protected Pair<Revision, Long> convertRevision(@NotNull VcsRevision rootRevision, @NotNull Project project) {
        Map<String, Revision> revisionMap = buildRevisionMap(rootRevision, project);
        long newRevisions = collectParents(rootRevision, revisionMap);

        return ImmutablePair.of(revisionMap.get(rootRevision.getInternalId()), newRevisions);
    }

    private long collectParents(@NotNull VcsRevision rootRevision, Map<String, Revision> revisionMap) {
        Set<String> importedRevisions = new HashSet<>(revisionMap.size());
        Queue<VcsRevision> revisionsToImport = new LinkedList<>();
        revisionsToImport.add(rootRevision);
        long newRevisions = 0;

        while (!revisionsToImport.isEmpty()) {
            VcsRevision revision = revisionsToImport.poll();
            String internalId = revision.getInternalId();
            if (importedRevisions.contains(internalId)) {
                LOGGER.trace("Encountered visited revision {}", internalId);
                continue;
            } else {
                importedRevisions.add(internalId);
            }
            Revision converted = revisionMap.get(internalId);
            if (converted == null) {
                LOGGER.trace("Revision {} is already imported", internalId);
                continue;
            }
            for (VcsRevision parent : revision.getParents()) {
                Revision convertedParent = revisionMap.get(parent.getInternalId());
                if (convertedParent == null) {
                    // The parent will be missing, because its child already exists
                    continue;
                }
                LOGGER.trace("Adding revision {} as parent of {}", parent.getInternalId(), internalId);
                converted.addParent(convertedParent);
                revisionsToImport.add(parent);
                newRevisions++;
            }
        }
        return newRevisions;
    }

    @NotNull
    private Map<String, Revision> buildRevisionMap(@NotNull VcsRevision rootRevision, @NotNull Project project) {
        Map<String, Revision> revisionMap = new HashMap<>();
        Queue<VcsRevision> revisionsToConvert = new LinkedList<>();
        revisionsToConvert.add(rootRevision);

        while (!revisionsToConvert.isEmpty()) {
            VcsRevision revision = revisionsToConvert.poll();
            validator.checkConstraints(revision);
            String internalId = revision.getInternalId();
            if (revisionMap.containsKey(internalId)) {
                // Already visited this revision - skip other parents
                LOGGER.trace("Found visited revision {}", internalId);
                continue;
            }
            Revision converted = revisionGraphService.findRevisionInProject(project, internalId);
            if (converted != null) {
                // Existing revision means that all parents have already been persisted - skip other parents
                LOGGER.trace("Found existing revision {} in database", internalId);
            } else {
                // Convert new revision and add all its parents to the queue
                converted = new Revision(revision).setProject(project);
                LOGGER.trace("Found new revision {}", internalId);
                revisionsToConvert.addAll(revision.getParents());
            }
            revisionMap.put(internalId, converted);
        }
        return revisionMap;
    }

    private Project indexDatabaseChanges(@NotNull Project project, @NotNull VcsLocalRepository localRepository) throws DatacTechnicalException {
        LOGGER.info("Begin indexing changes in project {}", project.getName());

        // Find revisions where only the changelog file itself changed to check if the given changelog file exists
        Collection<VcsRevision> changeLogRevisions = localRepository.listRevisionsWithChangesInPath(project.getChangelogLocation());

        if (changeLogRevisions.isEmpty()) {
            String msg = String.format("Couldn't find change log file %s for project %s", project.getChangelogLocation(), project.getName());
            eventLog.addMessage(new EventLogMessage(msg).setSeverity(MessageSeverity.WARNING));
            project.setState(ProjectState.MISSING_LOG);
            LOGGER.warn(msg);
            return project;
        }

        // For performing the actual indexing, retrieve all revisions which changed the whole directory in which the changelog lies
        Path parentPath = Paths.get(project.getChangelogLocation()).getParent();
        Collection<VcsRevision> changeLogDirectoryRevisions = localRepository.listRevisionsWithChangesInPath(parentPath.toString());

        project.setState(ProjectState.INDEXING);
        Project updatedProject = projectService.save(project);
        updateProjectState(updatedProject, 0);

        // Convert the external revisions to internal ones
        Collection<Revision> internalRevisions = revisionGraphService.findMatchingInternalRevisions(updatedProject, changeLogDirectoryRevisions);
        // Find those revisions which don't have a change set yet
        List<Revision> revisionsToIndex = new ArrayList<>();
        for (Revision revision : internalRevisions) {
            if (changeSetService.countByRevision(revision) == 0) {
                revisionsToIndex.add(revision);
            }
        }

        if (!revisionsToIndex.isEmpty()) {
            LOGGER.debug("Indexing {} revisions", revisionsToIndex.size());
            indexDatabaseChanges(updatedProject, localRepository, revisionsToIndex);
        } else {
            LOGGER.info("No new revisions in project {} [id={}]", updatedProject.getName(), updatedProject.getId());
        }

        return updatedProject;
    }

    /**
     * Performs indexing of database changes in the given project using the given local repository. The algorithm will
     * perform a breadth-first traversal from the root revision. For each revision during the traversal,
     * the whole local repository will checkout the given revision.
     * The traversal will abort when an already visited revision is met.
     *
     * @param project
     *         The project for which database changes shall be indexed.
     * @param localRepository
     *         The local repository connection.
     * @param revisionsToIndex
     *         The revisions that should be indexed.
     */
    private void indexDatabaseChanges(@NotNull Project project, @NotNull VcsLocalRepository localRepository, @NotNull Collection<Revision> revisionsToIndex) throws DatacTechnicalException {
        Revision rootRevision = revisionGraphService.findProjectRootRevision(project);
        AtomicInteger indexed = new AtomicInteger(0);
        AtomicInteger newChangeSets = new AtomicInteger(0);

        breadthFirstTraverser.traverseChildren(rootRevision, (Revision r) -> {
            if (revisionsToIndex.contains(r)) {
                double progress = (indexed.getAndIncrement() / (double) revisionsToIndex.size()) * 100.0;
                updateProjectState(project, progress);
                Collection<DatabaseChangeSet> changeSets = indexDatabaseChangesInRevision(project, localRepository, r);
                newChangeSets.addAndGet(changeSets.size());
            }
        });
        eventLog.addMessage(new EventLogMessage(String.format("Indexed total of %s new change sets", newChangeSets.get())));
        LOGGER.info("Indexed total of {} new change sets in project {} [id={}]", newChangeSets.get(), project.getName(), project.getId());
    }

    private Collection<DatabaseChangeSet> indexDatabaseChangesInRevision(Project project, VcsLocalRepository localRepository, Revision revision) throws DatacTechnicalException {
        LOGGER.debug("Indexing database changes of project {} in revision {}", project.getName(), revision.getInternalId());

        localRepository.checkoutRevision(revision);
        List<DatabaseChangeSet> databaseChangeSets = liquibaseProcessService.listDatabaseChangeSetsForProject(project);
        for (DatabaseChangeSet databaseChangeSet : databaseChangeSets) {
            databaseChangeSet.setRevision(revision);
        }

        return changeSetService.save(databaseChangeSets);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updateProjectState(@NotNull Project project, double progress) {
        applicationEventBus.publish(EventTopics.PROJECT_UPDATE, this, new ProjectUpdateEvent(project, progress));
    }
}
