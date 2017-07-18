package org.xlrnet.datac.vcs.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.domain.Branch;

import java.util.List;

/**
 * Repository for accessing branch data.
 */
public interface BranchRepository extends PagingAndSortingRepository<Branch, Long> {

    void deleteAllByProject(Project project);

    @Query("SELECT b FROM Branch b WHERE b.project = ?1 AND (b.watched = true OR b.development = true)")
    List<Branch> findAllWatchedOrDevelopmentByProject(Project project);
}
