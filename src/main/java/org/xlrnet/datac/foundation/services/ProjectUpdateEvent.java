package org.xlrnet.datac.foundation.services;

import org.xlrnet.datac.foundation.domain.Project;

/**
 * An event which indicates a change in a project's update state. This may e.g. be fired when a project update begins or
 * is finished.
 */
public class ProjectUpdateEvent {

    private final Project project;

    private final double progress;

    /**
     * Create a new project update event.
     *
     * @param project
     *         The project which is updated.
     * @param progress
     *         Update progress (if any).
     */
    public ProjectUpdateEvent(Project project, double progress) {
        this.project = project;
        this.progress = progress;
    }

    public ProjectUpdateEvent(Project project) {
        this(project, 0);
    }

    public Project getProject() {
        return project;
    }

    public double getProgress() {
        return progress;
    }
}
