package org.xlrnet.datac.database.domain.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.foundation.domain.Project;

/**
 * Repository for accessing deployment instances.
 */
public interface DeploymentInstanceRepository extends PagingAndSortingRepository<DeploymentInstance, Long> {

    @Query("SELECT i FROM DeploymentInstance i WHERE i.group.project = :project")
    @Transactional(readOnly = true)
    Set<DeploymentInstance> findAllByProject(@Param("project") Project project);
}
