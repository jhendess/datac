package org.xlrnet.datac.foundation.services;

import org.springframework.context.ApplicationEvent;
import org.xlrnet.datac.foundation.domain.Project;

import lombok.Getter;

/**
 * Event to indicate a required cache update of a single project.
 */
public class ProjectCacheReloadEvent extends ApplicationEvent {

    @Getter
    private final Project project;

    public ProjectCacheReloadEvent(Object source, Project project) {
        super(source);
        this.project = project;
    }
}
