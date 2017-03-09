package org.xlrnet.datac.foundation.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.repository.ProjectRepository;

/**
 * Transactional service for accessing project data.
 */
@Service
public class ProjectService extends AbstractTransactionalService<Project, ProjectRepository> {

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *
     * @param crudRepository
     *         The crud repository for providing basic crud operations.
     */
    @Autowired
    public ProjectService(ProjectRepository crudRepository) {
        super(crudRepository);
    }
}
