package org.xlrnet.datac.vcs.services;

import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.foundation.configuration.StartupPhases;
import org.xlrnet.datac.foundation.domain.EventLog;
import org.xlrnet.datac.foundation.domain.EventType;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.ProjectState;
import org.xlrnet.datac.foundation.services.EventLogService;
import org.xlrnet.datac.foundation.services.FileService;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.vcs.api.VcsLocalRepository;

import com.google.common.collect.ImmutableList;

/**
 * Lifecycle service which performs cleanups on application start and shutdown.
 */
@Component
@Scope("singleton")
public class CleanupProjectsInLifecycleService implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupProjectsInLifecycleService.class);

    private final EventLogService eventLogService;

    private final ProjectService projectService;

    private final VersionControlSystemService vcsService;

    private final FileService fileService;

    @Autowired
    public CleanupProjectsInLifecycleService(EventLogService eventLogService, ProjectService projectService, VersionControlSystemService vcsService, FileService fileService) {
        this.eventLogService = eventLogService;
        this.projectService = projectService;
        this.vcsService = vcsService;
        this.fileService = fileService;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void start() {
        LOGGER.info("Cleaning projects on startup");
        cleanAllProjects();
    }

    private void cleanAllProjects() {
        Iterable<Project> projects = projectService.findAll();
        // Clean projects in parallel
        StreamSupport.stream(projects.spliterator(), true)
                .forEach(this::cleanProject);
    }

    private void cleanProject(Project project) {
        if (!project.isInitialized()) {
            LOGGER.debug("Skipping uninitialized project {}", project.getName());
            return;
        }
        EventLog eventLog = eventLogService.newEventLog().setType(EventType.PROJECT_CLEANUP).setProject(project);
        Path projectRepositoryPath = fileService.getProjectRepositoryPath(project);
        try {
            LOGGER.debug("Cleaning project {}", project.getName());
            VcsLocalRepository repository = vcsService.getVcsAdapter(project).openLocalRepository(projectRepositoryPath, project);
            repository.cleanupIfNecessary();
            if (project.getState().isProgressable()) {
                project.setState(ProjectState.INTERRUPTED);
                projectService.save(project);
            }
        } catch (DatacTechnicalException e) {
            String msg = String.format("Cleaning project repository %s for project %s failed", projectRepositoryPath, project.getName());
            LOGGER.error(msg, e);
            project.setState(ProjectState.DIRTY);
            projectService.save(project);
            eventLogService.addExceptionToEventLog(eventLog, msg, e);
            eventLogService.save(eventLog);
        }
    }

    @Override
    public void stop() {
        // TODO: Check for projects in non-endstates and kindly kill them
        Collection<Project> runningProjects = projectService.findAllProjectsInState(ImmutableList.of(ProjectState.INDEXING, ProjectState.INITIALIZING, ProjectState.UPDATING));
        if (!runningProjects.isEmpty()) {
            LOGGER.warn("Projects are still updating");
        }
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public int getPhase() {
        return StartupPhases.CLEANUP;
    }
}
