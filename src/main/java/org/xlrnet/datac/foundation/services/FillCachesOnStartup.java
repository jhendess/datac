package org.xlrnet.datac.foundation.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.lifecycle.AbstractLifecycleComponent;
import org.xlrnet.datac.foundation.configuration.StartupPhases;
import org.xlrnet.datac.foundation.domain.Project;

import lombok.extern.slf4j.Slf4j;

/**
 * Lifecycle component which triggers a cache reload on startup.
 */
@Slf4j
@Component
public class FillCachesOnStartup extends AbstractLifecycleComponent {

    /** Access to all projects. */
    private final ProjectService projectService;

    /** Event publisher. */
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public FillCachesOnStartup(ProjectService projectService, ApplicationEventPublisher applicationEventPublisher) {
        this.projectService = projectService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    protected void onStart() {
        LOGGER.info("Filling caches on startup");
        Iterable<Project> projects = projectService.findAll();
        for (Project project : projects) {
            applicationEventPublisher.publishEvent(new ProjectCacheReloadEvent(this, project));
        }
        LOGGER.info("Finished cache warm up");
    }

    @Override
    public int getPhase() {
        return StartupPhases.WARMUP;
    }
}
