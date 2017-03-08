package org.xlrnet.datac.foundation.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.repository.ProjectRepository;
import org.xlrnet.datac.vcs.domain.repository.BranchRepository;

/**
 * Transactional service for accessing project data.
 */
@Service
public class ProjectService extends AbstractTransactionalService<Project, ProjectRepository> {

    private final BranchRepository branchRepository;

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *
     * @param crudRepository
     *         The crud repository for providing basic crud operations.
     * @param branchRepository
     */
    @Autowired
    public ProjectService(ProjectRepository crudRepository, BranchRepository branchRepository) {
        super(crudRepository);
        this.branchRepository = branchRepository;
    }

    /**
     * Projects
     * @param entity
     *         The entity to save.
     * @return
     */
    /*@Override
    @Transactional
    public Project save(Project entity) {
        Collection<Branch> branches = entity.getProject().getBranches();

        List<Branch> saved = new ArrayList<>(branches.size());
        for (Branch branch : branches) {
            if (branch != entity.getProject().getDevelopmentBranch()) {
                saved.add(branchRepository.save(branch));
            }
        }

        Branch devBranch = branchRepository.save(entity.getProject().getDevelopmentBranch());
        entity.getProject().setDevelopmentBranch(devBranch);
        entity.getProject().setBranches(saved);
        return getRepository().save(entity);
    }*/
}
