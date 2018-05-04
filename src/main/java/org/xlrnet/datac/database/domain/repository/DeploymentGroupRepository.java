package org.xlrnet.datac.database.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.xlrnet.datac.database.domain.DeploymentGroup;
import org.xlrnet.datac.foundation.domain.Project;

import java.util.Set;

/**
 * Repository for accessing deployment groups.
 */
public interface DeploymentGroupRepository extends PagingAndSortingRepository<DeploymentGroup, Long> {

    @Query("SELECT g FROM DeploymentGroup g WHERE g.project = :project AND g.parent IS NULL ORDER BY g.name")
    Set<DeploymentGroup> findRootGroupsByProject(@Param("project") Project project);

    Set<DeploymentGroup> findDeploymentGroupsByParentOrderByName(DeploymentGroup parent);

    int countByParent(DeploymentGroup parent);

    @Query("SELECT COUNT(g) FROM DeploymentGroup g WHERE g.project = :project AND g.parent IS NULL")
    int countRootGroupsByProject(@Param("project") Project project);
}
