package org.xlrnet.datac.database.domain.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.vcs.domain.Revision;

import java.util.List;

/**
 * Repository for accessing change set data.
 */
public interface ChangeSetRepository extends PagingAndSortingRepository<DatabaseChangeSet, Long> {

    List<DatabaseChangeSet> findAllByRevision(Revision revision);

    long countAllByRevision(Revision revision);
}
