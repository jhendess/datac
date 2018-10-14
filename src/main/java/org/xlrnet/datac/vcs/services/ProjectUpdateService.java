package org.xlrnet.datac.vcs.services;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.LockFailedException;
import org.xlrnet.datac.commons.exception.ProjectAlreadyInitializedException;
import org.xlrnet.datac.database.impl.liquibase.LiquibaseAdapter;
import org.xlrnet.datac.database.services.ChangeIndexingService;
import org.xlrnet.datac.database.services.ChangeSetService;
import org.xlrnet.datac.foundation.components.EventLogProxy;
import org.xlrnet.datac.foundation.domain.EventLogMessage;
import org.xlrnet.datac.foundation.domain.EventType;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.ProjectState;
import org.xlrnet.datac.foundation.services.EventLogService;
import org.xlrnet.datac.foundation.services.FileService;
import org.xlrnet.datac.foundation.services.ProjectCacheReloadEvent;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.vcs.api.VcsAdapter;
import org.xlrnet.datac.vcs.api.VcsConnectionStatus;
import org.xlrnet.datac.vcs.api.VcsLocalRepository;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;
import org.xlrnet.datac.vcs.api.VcsRevision;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;

/**
 * Service which is responsible for collecting all database changes in a project.
 */
@Service
public class ProjectUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectUpdateService.class);

    /**
     * Thread-scoped event log proxy.
     */
    private final EventLogProxy eventLog;

    /**
     * The VCS Adapter that will be used for updating the project.
     */
    private final VersionControlSystemRegistry vcsService;

    /**
     * Central locking service.
     */
    private final LockingService lockingService;

    /**
     * The file service for accessing file resources.
     */
    private final FileService fileService;

    /**
     * Transactional service for project data.
     */
    private final ProjectService projectService;

    /**
     * Service for updating branch data.
     */
    private final BranchService branchService;

    /**
     * Service for accessing the revision graph.
     */
    private final RevisionGraphService revisionGraphService;

    /**
     * Service for indexing database changes.
     */
    private final ChangeIndexingService changeIndexingService;

    /**
     * Event logging service.
     */
    private final EventLogService eventLogService;

    /**
     * Application event publisher.
     */
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ProjectUpdateService(EventLogProxy eventLog1, VersionControlSystemRegistry vcsService, LockingService lockingService, FileService fileService, ProjectService projectService, BranchService branchService, RevisionGraphService revisionGraphService, EventLogService eventLogService, EventLogProxy eventLog, LiquibaseAdapter databaseChangeSystemAdapter, ChangeSetService changeSetService, ChangeIndexingService changeIndexingService, ApplicationEventPublisher eventPublisher) {
        this.eventLog = eventLog1;
        this.vcsService = vcsService;
        this.lockingService = lockingService;
        this.fileService = fileService;
        this.projectService = projectService;
        this.branchService = branchService;
        this.revisionGraphService = revisionGraphService;
        this.eventLogService = eventLogService;
        this.changeIndexingService = changeIndexingService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Asynchronously trigger a project change update.
     *
     * @param project
     *         The project to update.
     */
    @Async
    public void startAsynchronousProjectUpdate(@NotNull Project project) {
        try {
            startProjectUpdate(project);
        } catch (LockFailedException e) {       // NOSONAR: No logging of exception necessary
            LOGGER.warn("Update of project {} [id={}] failed because project is locked", project.getName(), project.getId());
        }
    }

    void startProjectUpdate(@NotNull Project project) throws LockFailedException {
        if (lockingService.tryLock(project)) {
            try {
                LOGGER.info("Begin update of project {}", project.getName());
                eventLog.setDelegate(eventLogService.newEventLog().setType(EventType.PROJECT_UPDATE));
                Project reloaded = projectService.refresh(project);
                eventLog.setProject(reloaded);
                reloaded = updateProject(reloaded);
                if (reloaded.getState() != ProjectState.MISSING_LOG) {
                    LOGGER.info("Finished updating project {} [id={}] successfully", project.getName(), project.getId());
                    eventLog.addMessage(new EventLogMessage("Project update finished successfully"));
                }
            } catch (DatacTechnicalException | RuntimeException e) {
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
            throw new LockFailedException(project);
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
    private Project updateProject(@NotNull Project project) throws DatacTechnicalException {
        VcsAdapter vcsAdapter = vcsService.getVcsAdapter(project);
        project.setState(ProjectState.INITIALIZING);
        Project updatedProject = projectService.saveAndPublishStateChange(project, 0);

        try {
            if (!updatedProject.isInitialized()) {
                initializeProjectRepository(updatedProject, vcsAdapter);
            }

            Path repositoryPath = fileService.getProjectRepositoryPath(updatedProject);

            LOGGER.debug("Opening local repository at {}", repositoryPath.toString());
            VcsLocalRepository localRepository = vcsAdapter.openLocalRepository(repositoryPath, updatedProject);

            updatedProject = updateRevisions(updatedProject, localRepository);
            updatedProject = changeIndexingService.indexDatabaseChanges(updatedProject, localRepository);

            updatedProject.setLastChangeCheck(LocalDateTime.now());
            if (updatedProject.getState() != ProjectState.MISSING_LOG) {
                updatedProject.setState(ProjectState.FINISHED);
            }
            updatedProject = projectService.saveAndPublishStateChange(updatedProject, 0);
        } catch (RuntimeException | IOException e) {
            throw new DatacTechnicalException("Project update failed", e);
        } finally {
            eventPublisher.publishEvent(new ProjectCacheReloadEvent(this, updatedProject));
        }
        return updatedProject;
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
        updatedProject = projectService.saveAndPublishStateChange(updatedProject, 0);

        LOGGER.debug("Updating revisions in project {}", updatedProject.getName());

        long branchCount = updatedProject.getBranches().stream().filter(b -> b.isWatched() || b.isDevelopment()).count();
        int updatedBranches = 0;
        for (Branch branch : updatedProject.getBranches()) {
            projectService.saveAndPublishStateChange(updatedProject, updatedBranches / (double) branchCount * 100.f);
            if (branch.isWatched() || branch.isDevelopment()) {
                updateRevisionsInBranch(updatedProject, branch, localRepository);
                updatedBranches++;
            } else {
                LOGGER.debug("Skipping branch {} in project {}", branch.getName(), updatedProject.getName());
            }
        }
        LOGGER.debug("Finished revision update in project {}", updatedProject.getName());
        revisionGraphService.reloadRevisionCache(updatedProject);   // Update only the revision cache to improve indexing performance
        return updatedProject;
    }

    protected void updateRevisionsInBranch(@NotNull Project project, @NotNull Branch branch, @NotNull VcsLocalRepository localRepository) throws DatacTechnicalException {
        LOGGER.debug("Updating revisions on branch {} in project {}", branch.getName(), project.getName());
        localRepository.updateRevisionsFromRemote(branch);

        VcsRevision rootRevision = localRepository.listLatestRevisionOnBranch(branch);

        Pair<Revision, Long> latestRevision = revisionGraphService.convertRevisionAndSave(rootRevision, project);
        LOGGER.debug("Finished updating revisions on branch {} in project {}", branch.getName(), project.getName());
        branch.setInternalId(latestRevision.getLeft().getInternalId()); // Internal id must point to the correct revision
        branchService.save(branch);
        if (latestRevision.getRight() != null && latestRevision.getRight() > 0) {
            eventLog.addMessage(new EventLogMessage(String.format("Found %d new revisions in branch %s", latestRevision.getRight(), branch.getName())));
        }
    }
}
