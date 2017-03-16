package org.xlrnet.datac.foundation.services;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.repository.ProjectRepository;
import org.xlrnet.datac.vcs.services.LockingService;
import org.xlrnet.datac.vcs.services.VersionControlSystemService;
import org.xlrnet.datac.vcs.tasks.ProjectUpdateTask;

/**
 * Transactional service for accessing project data.
 */
@Service
public class ProjectService extends AbstractTransactionalService<Project, ProjectRepository> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectService.class);

    /** The central locking service. */
    private final LockingService lockingService;

    /** The task executor. */
    private final TaskExecutor taskExecutor;

    /** The VCS service. */
    private final VersionControlSystemService vcsService;

    /** The file service. */
    private final FileService fileService;

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *  @param crudRepository
     *         The crud repository for providing basic crud operations.
     * @param lockingService
     * @param taskExecutor
     * @param vcsService
     * @param fileService
     */
    @Autowired
    public ProjectService(ProjectRepository crudRepository, LockingService lockingService, TaskExecutor taskExecutor, VersionControlSystemService vcsService, FileService fileService) {
        super(crudRepository);
        this.lockingService = lockingService;
        this.taskExecutor = taskExecutor;
        this.vcsService = vcsService;
        this.fileService = fileService;
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
        if (!lockingService.isLocked(project)) {
            ProjectUpdateTask projectUpdateTask = new ProjectUpdateTask(project, vcsService, lockingService, fileService);
            taskExecutor.execute(projectUpdateTask);
            LOGGER.debug("Successfully queued update task for project {} [{}]", project.getId(), project.getName());
            return true;
        }
        LOGGER.debug("Queuing update task for project {} [{}] failed: project is locked", project.getId(), project.getName());
        return false;
    }
}
