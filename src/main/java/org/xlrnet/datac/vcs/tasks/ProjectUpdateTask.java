package org.xlrnet.datac.vcs.tasks;

import static com.google.common.base.Preconditions.checkArgument;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.commons.tasks.AbstractRunnableTask;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.services.ProjectUpdateService;

import lombok.extern.slf4j.Slf4j;

/**
 * Task which performs an update on a given project.
 */
@Slf4j
public class ProjectUpdateTask extends AbstractRunnableTask<Project> {

    /**
     * Service for performing the project update.
     */
    private final ProjectUpdateService updateService;

    /**
     * The project that will be updated.
     */
    private Project projectToUpdate;

    public ProjectUpdateTask(@NotNull ProjectUpdateService updateService) {
        this.updateService = updateService;
    }

    public void setProjectToUpdate(Project projectToUpdate) {
        this.projectToUpdate = projectToUpdate;
    }

    @Override
    protected void runTask() {
        checkArgument(projectToUpdate != null, "Project to update may not be null");
        // Start the update asynchronously to make sure that all thread-scoped properties are initialized correctly
        updateService.startAsynchronousProjectUpdate(projectToUpdate);
    }
}
