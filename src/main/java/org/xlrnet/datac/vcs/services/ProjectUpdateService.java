package org.xlrnet.datac.vcs.services;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import javax.transaction.Transactional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.vaadin.spring.events.EventBus;
import org.xlrnet.datac.commons.domain.BreadthFirstTraverser;
import org.xlrnet.datac.commons.domain.DepthFirstTraverser;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.ProjectAlreadyInitializedException;
import org.xlrnet.datac.commons.exception.VcsRepositoryException;
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
     * Helper class for performing breadth first traversals on revision graphs.
     */
    private BreadthFirstTraverser<Revision> breadthFirstTraverser = new BreadthFirstTraverser<>();

    /**
     * Helper class for performing depth first traversals on revision graphs.
     */
    private DepthFirstTraverser<Revision> depthFirstTraverser = new DepthFirstTraverser<>();

    @Autowired
    public ProjectUpdateService(EventBus.ApplicationEventBus applicationEventBus, VersionControlSystemService vcsService, LockingService lockingService, FileService fileService, ProjectService projectService, RevisionGraphService revisionGraphService, ValidationService validator, EventLogService eventLogService, EventLogProxy eventLog, LiquibaseProcessService liquibaseProcessService) {
        this.applicationEventBus = applicationEventBus;
        this.vcsService = vcsService;
        this.lockingService = lockingService;
        this.fileService = fileService;
        this.projectService = projectService;
        this.revisionGraphService = revisionGraphService;
        this.validator = validator;
        this.eventLogService = eventLogService;
        this.eventLog = eventLog;
        this.liquibaseProcessService = liquibaseProcessService;
    }

    /**
     * Asynchronously trigger a project change update.
     *
     * @param project
     *         The project to update.
     */
    @Async
    public void startAsynchronousProjectUpdate(@NotNull Project project) {
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

        VcsRevision rootRevision = localRepository.fetchLatestRevisionInBranch(branch);

        Pair<Revision, Long> revision = convertRevision(rootRevision, project);
        LOGGER.trace("Saving revisions on branch {} in project {}", branch.getName(), project.getName());
        revisionGraphService.save(revision.getLeft());
        LOGGER.debug("Finished updating revisions on branch {} in project {}", branch.getName(), project.getName());
        if (revision.getRight() != null && revision.getRight() > 0) {
            eventLog.addMessage(new EventLogMessage(String.format("Found %d new revisions in branch %s", revision.getRight(), branch.getName())));
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

        // Find revisions where only the changelog file itself changed
        Collection<VcsRevision> changeLogRevisions = localRepository.listRevisionsWithChangesInPath(project.getChangelogLocation());

        if (changeLogRevisions.isEmpty()) {
            eventLog.addMessage(new EventLogMessage("Changelog file " + project.getChangelogLocation() + " could not be found").setSeverity(MessageSeverity.WARNING));
            project.setState(ProjectState.MISSING_LOG);
            LOGGER.warn("Couldn't find change log file {} for project {}", project.getChangelogLocation(), project.getName());
            return project;
        }

        project.setState(ProjectState.INDEXING);
        project = projectService.save(project);
        updateProjectState(project, 0);

        // Convert the external revisions to internal ones
        Collection<Revision> internalChangeLogRevisions = revisionGraphService.findMatchingInternalRevisions(project, changeLogRevisions);

        // Find the closest revisions to the root revision; this way we don't need to parse through every possible revision
        // TODO: Begin after the last indexed revisions
        Revision rootRevision = revisionGraphService.findProjectRootRevision(project);
        Collection<Revision> bestRevisionsToBeginIndexing = findFirstRevisionsAfterRevision(rootRevision, internalChangeLogRevisions);

        LOGGER.trace("Begin with revisions {} for indexing", bestRevisionsToBeginIndexing);

        // TODO: As of now we can't just index the revisions where a change happened, since we cannot detect included changes at the moment

        indexDatabaseChanges(project, localRepository, bestRevisionsToBeginIndexing);

        return project;
    }

    /**
     * Performs indexing of database changes in the given project using the given local repository. The algorithm will
     * perform a depth-first traversal by the children of the given revisions. For each revision during the traversal,
     * the whole local repository will checkout the given revision.
     * The traversal will abort when an already visited revision is met.
     *
     * @param project
     *         The project for which database changes shall be indexed.
     * @param localRepository
     *         The local repository connection.
     * @param beginIndexing
     *         The revisions where indexing should begin.
     */
    private void indexDatabaseChanges(@NotNull Project project, @NotNull VcsLocalRepository localRepository, @NotNull Collection<Revision> beginIndexing) throws DatacTechnicalException {
        List<Revision> revisionsToIndex = new ArrayList<>();    // Hold all revisions to index in this list to be able to provide a progress indicator
        Set<Revision> visitedRevisions = new HashSet<>();
        for (Revision revision : beginIndexing) {
            depthFirstTraverser.traverseChildrenAbortOnCondition(revision,
                    (r -> !visitedRevisions.contains(r)),
                    (r) -> {
                        visitedRevisions.add(r);
                        revisionsToIndex.add(r);
                    });
        }

        int indexed = 0;
        for (Revision toIndex : revisionsToIndex) {
            double progress = (indexed / (double) revisionsToIndex.size()) * 100.0;
            updateProjectState(project, progress);
            indexDatabaseChangesInRevision(project, localRepository, toIndex);
            indexed++;
        }

    }

    private void indexDatabaseChangesInRevision(Project project, VcsLocalRepository localRepository, Revision r) throws VcsRepositoryException {
        LOGGER.debug("Indexing database changes of project {} in revision {}", project.getName(), r.getInternalId());

        localRepository.checkoutRevision(r);
        // TODO: Perform the actual indexing
    }

    /**
     * Find those children which appear first after a given revision. This performs a breadth-first traversal of child
     * elements. The elements will be returned ordered by their distance to the given root node (i.e. the first element
     * will be the closest, and the last the farthest). If one element on a single branch in this graph matches, all
     * succeeding elements won't be returned.
     *
     * @param revision
     *         The revision whose children will be traversed.
     * @param revisionsToFind
     *         The revisions that will be searched for.
     * @return Those revisions of the given collection which appear closest to the given revision itself.
     */
    @NotNull
    List<Revision> findFirstRevisionsAfterRevision(@NotNull Revision revision, Collection<Revision> revisionsToFind) throws DatacTechnicalException {
        List<Revision> revisionsToBeginWith = new ArrayList<>();
        breadthFirstTraverser.traverseChildrenCutOnMatch(revision, revisionsToFind::contains, revisionsToBeginWith::add);
        return revisionsToBeginWith;
    }

    private void updateProjectState(@NotNull Project project, double progress) {
        applicationEventBus.publish(EventTopics.PROJECT_UPDATE, this, new ProjectUpdateEvent(project, progress));
    }
}
