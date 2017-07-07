package org.xlrnet.datac.database.domain.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.domain.Revision;

/**
 * Repository for accessing change set data.
 */
public interface ChangeSetRepository extends PagingAndSortingRepository<DatabaseChangeSet, Long> {

    List<DatabaseChangeSet> findAllByRevision(Revision revision);

    long countAllByRevision(Revision revision);

    /**
     * Finds the change set which introduced this change set. This requires that change sets are always written
     * beginning by the oldest. The change set for the introducing changeset will be determined, may not yet be
     * written to database.
     */
    @Query("SELECT d FROM DatabaseChangeSet d WHERE d.revision.project = ?1 AND d.internalId = ?2 AND d.sourceFilename = ?3 AND d.introducingChangeSet IS NULL")
    DatabaseChangeSet findIntroducingChangeSet(Project project, String internalId, String sourceFileName);

    /**
     * Finds the change set which will be overwritten by this change set. This requires that change sets are always
     * written beginning by the oldest. The change set for the introducing change set will be determined, may not yet be
     * written to database.
     */
    @Query("SELECT d FROM DatabaseChangeSet d WHERE d.revision.project = ?1 AND d.internalId = ?2 AND d.sourceFilename = ?3 AND d.checksum <> ?4 AND d.conflictingChangeSet IS NULL")
    Collection<DatabaseChangeSet> findOverwrittenChangeSets(Project project, String internalId, String sourceFileName, String checksum);

}
