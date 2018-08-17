package org.xlrnet.datac.administration.services;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.administration.tasks.ChecksumRecalculationTask;
import org.xlrnet.datac.administration.util.MaintenanceModeEvent;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.commons.tasks.RunnableTask;
import org.xlrnet.datac.database.services.ChangeIndexingService;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.FileService;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.vcs.services.LockingService;
import org.xlrnet.datac.vcs.services.RevisionGraphService;
import org.xlrnet.datac.vcs.services.VersionControlSystemRegistry;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for starting maintenance work in the application.
 */
@Slf4j
@Service
@Scope("singleton")
public class ApplicationMaintenanceService {

    /** Number of times the application will try to lock projects. */
    private static final int MAX_LOCK_FAILED_ITERATIONS = 10;

    /** Time in milliseconds between a lock attempt retry. */
    private static final int MS_BETWEEN_LOCK_ATTEMPTS = 5000;

    /** Service for locking projects. */
    private final LockingService lockingService;

    /** Task executor. */
    private final TaskExecutor executorService;

    /** Service for accessing project data. */
    private final ProjectService projectService;

    private final ChangeIndexingService indexingService;

    private final VersionControlSystemRegistry vcsRegistry;

    private final FileService fileService;

    private final RevisionGraphService revisionGraphService;

    private AtomicBoolean maintenanceModeEnabled = new AtomicBoolean(false);

    /**
     * Application event publisher.
     */
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ApplicationMaintenanceService(LockingService lockingService, @Qualifier("defaultTaskExecutor") TaskExecutor executorService, ProjectService projectService, ChangeIndexingService indexingService, VersionControlSystemRegistry vcsRegistry, FileService fileService, RevisionGraphService revisionGraphService, ApplicationEventPublisher eventPublisher) {
        this.lockingService = lockingService;
        this.executorService = executorService;
        this.projectService = projectService;
        this.indexingService = indexingService;
        this.vcsRegistry = vcsRegistry;
        this.fileService = fileService;
        this.revisionGraphService = revisionGraphService;
        this.eventPublisher = eventPublisher;
    }

    public boolean startChecksumRecalculation() {
        if (!isMaintenanceModeEnabled()) {
            MaintenanceOperationStatusObserver observer = new MaintenanceOperationStatusObserver(this);
            ChecksumRecalculationTask checksumRecalculationTask = new ChecksumRecalculationTask(projectService, indexingService, vcsRegistry, revisionGraphService, fileService);
            prepareAndExecuteMaintenanceTask(observer, checksumRecalculationTask);
            return true;
        }
        return false;
    }

    void prepareAndExecuteMaintenanceTask(MaintenanceOperationStatusObserver observer, RunnableTask checksumRecalculationTask) {
        checksumRecalculationTask.setProgressChangeHandler(observer);
        checksumRecalculationTask.setRunningStatusHandler(observer);
        executorService.execute(checksumRecalculationTask);
    }

    public boolean isMaintenanceModeEnabled() {
        return maintenanceModeEnabled.get();
    }

    /**
     * Start application-wide maintenance mode and fire a {@link MaintenanceModeEvent} to notify all components.
     * Calling this method will lock all projects.
     */
    void startMaintenanceMode() {
        LOGGER.info("Starting maintenance mode");
        try {
            maintenanceModeEnabled.set(true);
            lockProjects();
            eventPublisher.publishEvent(new MaintenanceModeEvent(this, true));
            LOGGER.info("Maintenance mode is now enabled");
        } catch (RuntimeException e) {
            LOGGER.error("Starting maintenance mode failed", e);
            stopMaintenanceMode();
        }
    }

    /**
     * Stop application-wide maintenance mode and fire a {@link MaintenanceModeEvent} to notify all components.
     * Calling this method will unlock all projects.
     */
    void stopMaintenanceMode() {
        LOGGER.info("Stopping maintenance mode");
        maintenanceModeEnabled.set(false);
        try {
            unlockProjects();
            eventPublisher.publishEvent(new MaintenanceModeEvent(this, false));
        } finally {
            LOGGER.info("Maintenance mode is now disabled");
        }
    }

    private void unlockProjects() {
        for (Project project : projectService.findAllAlphabetically()) {
            lockingService.unlock(project);
        }
    }

    private void lockProjects() {
        int lockFailed = 0;
        Collection<Project> allProjects = projectService.findAllAlphabetically();
        Collection<Project> projectsToLock = Lists.newArrayList(allProjects);
        while (!projectsToLock.isEmpty()) {
            for (Iterator<Project> iterator = projectsToLock.iterator(); iterator.hasNext(); ) {
                Project project = iterator.next();
                boolean lockSuccessful = lockingService.tryLock(project);
                if (lockSuccessful) {
                    iterator.remove();
                } else {
                    LOGGER.warn("Locking project {} [id={}] failed - trying again later", project.getName(), project.getId());
                }
            }
            if (lockFailed == MAX_LOCK_FAILED_ITERATIONS) {
                throw new DatacRuntimeException("Locking projects failed more than " + MAX_LOCK_FAILED_ITERATIONS + " times");
            }
            if (!projectsToLock.isEmpty()) {
                lockFailed++;
                try {
                    LOGGER.warn("Waiting {} ms before trying to lock projects ({}/{})", MS_BETWEEN_LOCK_ATTEMPTS, lockFailed, MAX_LOCK_FAILED_ITERATIONS);
                    Thread.sleep(MS_BETWEEN_LOCK_ATTEMPTS);
                } catch (InterruptedException e) {
                    LOGGER.warn("Unexpected interrupt while waiting for acquire more locks", e);
                    throw new DatacRuntimeException("Unexpected interrupt while waiting for acquire more locks", e);
                }
            }
        }

    }
}
