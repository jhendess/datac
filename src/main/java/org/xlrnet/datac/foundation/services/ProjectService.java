package org.xlrnet.datac.foundation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.repository.ProjectRepository;
import org.xlrnet.datac.vcs.services.LockingService;
import org.xlrnet.datac.vcs.services.VersionControlSystemService;

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
}
