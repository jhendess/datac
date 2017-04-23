package org.xlrnet.datac.database.domain.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.vcs.domain.Revision;

/**
 * Repository for accessing change set data.
 */
public interface ChangeSetRepository extends PagingAndSortingRepository<DatabaseChangeSet, Long> {

    Iterable<DatabaseChangeSet> findAllByRevision(Revision revision);

    long countAllByRevision(Revision revision);
}
