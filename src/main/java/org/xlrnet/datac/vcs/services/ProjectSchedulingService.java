package org.xlrnet.datac.vcs.services;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.commons.lifecycle.AbstractLifecycleComponent;
import org.xlrnet.datac.foundation.configuration.StartupPhases;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.repository.ProjectRepository;
import org.xlrnet.datac.vcs.tasks.ProjectUpdateTask;

import lombok.extern.slf4j.Slf4j;

/**
 * Service which is responsible for scheduling automatic project updates.
 */
@Slf4j
@Service
public class ProjectSchedulingService extends AbstractLifecycleComponent {

    /**
     * Information about the currently running environment.
     */
    private final Environment environment;

    /**
     * Service for accessing project data.
     */
    private final ProjectRepository projectRepository;

    /**
     * Scheduling service.
     */
    private final TaskScheduler taskScheduler;

    /**
     * Bean factory which supports manual autowiring of custom beans.
     */
    private final AutowireCapableBeanFactory beanFactory;

    private Map<Long, ScheduledFuture> projectScheduleMap = new HashMap<>();

    @Autowired
    public ProjectSchedulingService(Environment environment, ProjectRepository projectRepository, TaskScheduler taskScheduler, AutowireCapableBeanFactory beanFactory) {
        this.environment = environment;
        this.projectRepository = projectRepository;
        this.taskScheduler = taskScheduler;
        this.beanFactory = beanFactory;
    }

    @Override
    protected void onStart() {
        if (!environment.acceptsProfiles("test", "development")) {
            LOGGER.info("Scheduling automatic project updates");
            scheduleAllProjects();
        } else {
            LOGGER.warn("Automatic project update scheduling is DISABLED in current profile");
        }
    }

    private void scheduleAllProjects() {
        Iterable<Project> projects = projectRepository.findAll();
        for (Project project : projects) {
            scheduleProjectUpdate(project);
        }
    }

    /**
     * Setup scheduling for a given project using its internal interval settings defined in
     * {@link Project#getPollInterval()}. Existing schedules will not be removed.
     *
     * @param project
     *         The project to schedule.
     *         @return Flag whether the project was scheduled for an update.
     */
    public boolean scheduleProjectUpdate(@NotNull Project project) {
        boolean scheduled = false;
        if (project.isAutomaticPollingEnabled()) {
            LOGGER.info("Scheduling update of project {} [id={}] in {} minute interval", project.getName(), project.getId(), project.getPollInterval());
            ProjectUpdateTask task = beanFactory.createBean(ProjectUpdateTask.class);
            task.setProjectToUpdate(project);
            PeriodicTrigger trigger = new PeriodicTrigger(project.getPollInterval(), TimeUnit.MINUTES);
            trigger.setInitialDelay(project.getPollInterval());
            ScheduledFuture<?> schedule = taskScheduler.schedule(task, trigger);
            if (schedule != null) {
                projectScheduleMap.put(project.getId(), schedule);
                LOGGER.debug("Successfully scheduled automatic project update for {} [id={}]", project.getName(), project.getId());
                scheduled = true;
            } else {
                LOGGER.error("Scheduling project update for {} [id={}] failed", project.getName(), project.getId());
            }
        } else {
            LOGGER.info("Project {} [id={}] will not be not scheduled for automatic polling", project.getName(), project.getId());
        }
        return scheduled;
    }

    public boolean unscheduleProjectUpdate(@NotNull Project project) {
        LOGGER.info("Unscheduling automatic update of project {} [id={}]", project.getName(), project.getId());
        ScheduledFuture scheduledFuture = projectScheduleMap.get(project.getId());
        if (scheduledFuture == null) {
            LOGGER.info("No automatic update scheduled for project {} [id={}]", project.getName(), project.getId());
            return false;
        }
        boolean cancelled = scheduledFuture.cancel(true);
        if (cancelled) {
            LOGGER.debug("Unscheduled automatic update of project {} [id={}] successfully", project.getName(), project.getId());
        } else {
            LOGGER.error("Unscheduling automatic update of project {} [id={}] failed for unknown reason", project.getName(), project.getId());
        }
        return cancelled;
    }

    @Override
    protected void onStop() {
        LOGGER.info("Unscheduling project updates");
        for (ScheduledFuture scheduledFuture : projectScheduleMap.values()) {
            scheduledFuture.cancel(true);
        }
    }

    @Override
    public int getPhase() {
        return StartupPhases.SCHEDULING;
    }
}
