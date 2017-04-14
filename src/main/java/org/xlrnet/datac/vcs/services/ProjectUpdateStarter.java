package org.xlrnet.datac.vcs.services;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.foundation.domain.Project;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Simple service used for triggering a project update if possible.
 */
@Service
public class ProjectUpdateStarter {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProjectUpdateStarter.class);

    private final ProjectUpdateService projectUpdateService;

    private final LockingService lockingService;

    @Autowired
    public ProjectUpdateStarter(ProjectUpdateService projectUpdateService, LockingService lockingService) {
        this.projectUpdateService = projectUpdateService;
        this.lockingService = lockingService;
    }

    /**
     * Try to queue a new project update. If the project is currently locked for writing, no update will be queued.
     *
     * @param project
     *         The project that should be updated.
     * @return True if the project update could be queued successfully or false if the project is currently locked for
     * writing.
     */
    public boolean queueProjectUpdate(@NotNull Project project) {
        checkArgument(project.isPersisted(), "Project must be persisted");

        if (!lockingService.isLocked(project)) {
            projectUpdateService.startAsynchronousProjectUpdate(project);
            LOGGER.debug("Successfully queued update task for project {} [id={}]", project.getId(), project.getName());
            return true;
        }
        LOGGER.debug("Queuing update task for project {} [id={}] failed: project is locked", project.getName(), project.getId());
        return false;
    }
}
