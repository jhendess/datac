package org.xlrnet.datac.foundation.services;

import org.xlrnet.datac.foundation.domain.Project;

/**
 * An event which indicates a change in a project's update state. This may e.g. be fired when a project update begins or
 * is finished.
 */
public class ProjectUpdateEvent {

    /**
     * Create a new project update event.
     *
     * @param project
     *         The project which is updated.
     */
    public ProjectUpdateEvent(Project project) {
        this.project = project;
    }

    private final Project project;

    public Project getProject() {
        return project;
    }
}
