package org.xlrnet.datac.foundation.services;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.spring.events.EventBus;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.foundation.EventTopics;
import org.xlrnet.datac.foundation.components.EventLogProxy;
import org.xlrnet.datac.foundation.domain.EventLogMessage;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.ProjectCredentialsEncryptionListener;
import org.xlrnet.datac.foundation.domain.ProjectState;
import org.xlrnet.datac.foundation.domain.repository.ProjectRepository;
import org.xlrnet.datac.vcs.api.VcsConnectionException;
import org.xlrnet.datac.vcs.api.VcsLocalRepository;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.services.BranchService;
import org.xlrnet.datac.vcs.services.ProjectSchedulingService;

import java.util.Collection;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Transactional service for accessing project data.
 */
@Service
public class ProjectService extends AbstractTransactionalService<Project, ProjectRepository> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectService.class);

    /**
     * The application event bus.
     */
    private final EventBus.ApplicationEventBus applicationEventBus;

    /**
     * The file service.
     */
    private final FileService fileService;

    /**
     * The request-scoped event log (if any).
     */
    private final EventLogProxy eventLog;

    /**
     * Service for manipulating branches.
     */
    private final BranchService branchService;

    /**
     * Service for scheduling automatic project updates.
     */
    private final ProjectSchedulingService projectSchedulingService;

    /**
     * Since hibernate only calls entity listeners when a non-transient field has changed, we have to call the listener
     * manually on save operations which is pretty ugly imho...
     */
    private final ProjectCredentialsEncryptionListener projectCredentialsEncryptionListener;

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     */
    @Autowired
    public ProjectService(ProjectRepository crudRepository, EventBus.ApplicationEventBus applicationEventBus, FileService fileService, EventLogProxy eventLog, BranchService branchService, ProjectSchedulingService projectSchedulingService, ProjectCredentialsEncryptionListener projectCredentialsEncryptionListener) {
        super(crudRepository);
        this.applicationEventBus = applicationEventBus;
        this.fileService = fileService;
        this.eventLog = eventLog;
        this.branchService = branchService;
        this.projectSchedulingService = projectSchedulingService;
        this.projectCredentialsEncryptionListener = projectCredentialsEncryptionListener;
    }

    /**
     * Finds all projects in the given {@link ProjectState}.
     *
     * @param states
     *         The states to look for.
     * @return All projects in that state.
     */
    @Transactional(readOnly = true)
    public Collection<Project> findAllProjectsInState(Iterable<ProjectState> states) {
        return getRepository().findAllByStateIn(states);
    }


    @Transactional
    public Project updateAvailableBranches(@NotNull Project project, @NotNull VcsLocalRepository localRepository) throws VcsConnectionException {
        Pattern pattern = Pattern.compile(project.getNewBranchPattern());
        VcsRemoteRepositoryConnection remoteRepositoryConnection = localRepository.connectToRemote();
        Collection<Branch> remoteBranches = remoteRepositoryConnection.listBranches();
        for (Branch remoteBranch : remoteBranches) {
            // Check if this remote branch doesn't exist yet
            if (project.getBranches().stream()
                    .noneMatch((branch -> StringUtils.equals(branch.getName(), remoteBranch.getName())))) {
                if (pattern.matcher(remoteBranch.getName()).matches()) {
                    LOGGER.debug("Found new matching branch {} [id={}]", remoteBranch.getName(), remoteBranch.getInternalId());
                    remoteBranch.setWatched(true);
                    eventLog.addMessage(new EventLogMessage("Found new matching branch " + remoteBranch.getName()));
                } else {
                    LOGGER.trace("Branch {} [id={}] is new but doesn't match pattern {}", remoteBranch.getName(), remoteBranch.getInternalId(), pattern.toString());
                    eventLog.addMessage(new EventLogMessage("Found new non-matching branch " + remoteBranch.getName()));
                }
                project.addBranch(remoteBranch);
            } else {
                LOGGER.trace("Branch {} [id={}] is not new", remoteBranch.getName(), remoteBranch.getInternalId());
            }
        }
        return save(project);
    }

    /**
     * Performs a clean delete of a project which removes both the entity from the database and working directories from
     * the filesystem.
     *
     * @param entity
     *         The project to delete.
     * @throws DatacTechnicalException
     *         Will be thrown in case of an error while deleting the directory structure.
     */
    @Transactional
    public void deleteClean(Project entity) throws DatacTechnicalException {
        fileService.deleteProjectRepository(entity);
        projectSchedulingService.unscheduleProjectUpdate(entity);
        super.delete(entity);
    }

    /**
     * Reloads the given project and marks it as failed. Any changes to the given entity won't be persisted. Runs in a
     * separate transaction and fires a project update event.
     *
     * @param project
     *         The project to mark as failed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Project markProjectAsFailedUpdate(Project project) {
        checkArgument(project.isPersisted(), "Entity is not persisted");
        Project reloaded = super.findOne(project.getId());
        reloaded.setState(ProjectState.ERROR);
        return saveAndPublishStateChange(reloaded, 0.0f);
    }

    /**
     * Saves the project and publishes the latest project state change on the application event bus.
     *
     * @param project
     *         The project to save.
     * @param progress
     *         The new progress.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Project saveAndPublishStateChange(@NotNull Project project, double progress) {
        Project saved = save(project);
        applicationEventBus.publish(EventTopics.PROJECT_UPDATE, this, new ProjectUpdateEvent(saved, progress));
        return saved;
    }

    @Transactional
    public Project saveProject(Project project) {
        if (project.isPersisted()) {
            branchService.deleteByProject(project);
        }
        Project saved = save(project);
        if (saved != null) {
            if (project.isPersisted()) {
                projectSchedulingService.unscheduleProjectUpdate(saved);
            }
            projectSchedulingService.scheduleProjectUpdate(saved);
        }
        return saved;
    }

    @Override
    public <S extends Project> S save(S entity) {
        projectCredentialsEncryptionListener.encrypt(entity);
        return super.save(entity);
    }
}
