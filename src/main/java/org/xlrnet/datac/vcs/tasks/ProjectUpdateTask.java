package org.xlrnet.datac.vcs.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlrnet.datac.commons.tasks.AbstractRunnableTask;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.services.ProjectUpdateService;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Task which performs an update on a given project.
 */
public class ProjectUpdateTask extends AbstractRunnableTask<Project> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectUpdateTask.class);

    /**
     * Service for performing the project update.
     */
    private final ProjectUpdateService updateService;

    /**
     * The project that will be updated.
     */
    private Project projectToUpdate;

    public ProjectUpdateTask(ProjectUpdateService updateService) {
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
