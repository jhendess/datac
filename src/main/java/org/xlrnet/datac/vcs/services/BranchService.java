package org.xlrnet.datac.vcs.services;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.repository.BranchRepository;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Service for accessing and manipulating branches.
 */
@Service
public class BranchService extends AbstractTransactionalService<Branch, BranchRepository> {

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *
     * @param crudRepository
     *         The crud repository for providing basic crud operations.
     */
    @Autowired
    public BranchService(BranchRepository crudRepository) {
        super(crudRepository);
    }

    /**
     * Removes existing branches from the database to prevent duplicate branches when saving a project. It checks only
     * based on the persisted project ID. This method must be called from within another transaction which corrects the
     * branches - otherwise constraints will be violated.
     *
     * @param projectBean
     *         The project whose branches should be removed.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteByProject(@NotNull Project projectBean) {
        checkArgument(projectBean.isPersisted());
        getRepository().deleteAllByProject(projectBean);
    }

    /**
     * Returns all watched branches in the given project.
     *
     * @param project
     *         The project in which the branches must be.
     * @return All branhes in the given project.
     */
    @Transactional(readOnly = true)
    public List<Branch> findAllWatchedByProject(@NotNull Project project) {
        return getRepository().findAllWatchedOrDevelopmentByProject(project);
    }
}
