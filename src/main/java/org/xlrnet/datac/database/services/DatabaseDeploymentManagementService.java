package org.xlrnet.datac.database.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.database.domain.DeploymentGroup;
import org.xlrnet.datac.database.domain.repository.DeploymentGroupRepository;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;

import java.util.Set;

/**
 * Service for managing deployment groups.
 */
@Slf4j
@Service
public class DatabaseDeploymentManagementService extends AbstractTransactionalService<DeploymentGroup, DeploymentGroupRepository> {

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *
     * @param crudRepository
     *         The crud repository for providing basic crud operations.
     */
    @Autowired
    public DatabaseDeploymentManagementService(DeploymentGroupRepository crudRepository) {
        super(crudRepository);
    }

    /**
     * Returns all root deployment groups for a given project. A root group is a deployment group without any parent
     * group. If there are none, an empty set will be returned.
     *
     * @param project
     *         The project in which the root groups must lie.
     * @return A set of deployment groups.
     */
    @Transactional(readOnly = true)
    public Set<DeploymentGroup> findRootGroupsByProject(Project project) {
        return getRepository().findRootGroupsByProject(project);
    }

    @Transactional(readOnly = true)
    public Set<DeploymentGroup> findDeploymentGroupsByParent(DeploymentGroup parent) {
        return getRepository().findDeploymentGroupsByParent(parent);
    }

    @Transactional(readOnly = true)
    public boolean hasChildGroups(DeploymentGroup deploymentGroup) {
        return getRepository().countByParent(deploymentGroup) > 0;
    }
}
