package org.xlrnet.datac.database.domain.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.xlrnet.datac.database.domain.DeploymentInstance;

/**
 * Repository for accessing deployment instances.
 */
public interface DeploymentInstanceRepository extends PagingAndSortingRepository<DeploymentInstance, Long> {

}
