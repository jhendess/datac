package org.xlrnet.datac.database.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.database.domain.DeploymentGroup;
import org.xlrnet.datac.database.domain.repository.DeploymentGroupRepository;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;

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
}
