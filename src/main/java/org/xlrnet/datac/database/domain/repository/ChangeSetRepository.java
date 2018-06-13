package org.xlrnet.datac.database.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
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

    @Query(value = "SELECT COUNT(*) FROM CHANGESET WHERE REVISION_ID = ?1", nativeQuery = true)
    long countAllByRevisionId(Long revisionId);

    /**
     * Counts all change sets which were modified the given change set.
     *
     * @param changeSet
     *         The change set to change for overwrites.
     * @return The number of change sets conflicting with the given changeset.
     */
    @Query("SELECT count(d) FROM DatabaseChangeSet d WHERE d.introducingChangeSet = ?1 AND d.modifying = true")
    long countModifyingChangeSets(DatabaseChangeSet changeSet);

    /**
     * Finds the change set which introduced this change set. This requires that change sets are always written
     * beginning by the oldest. The change set for the introducing changeset will be determined, may not yet be written
     * to database.
     */
    @Query("SELECT d FROM DatabaseChangeSet d WHERE d.revision.project = ?1 AND d.internalId = ?2 AND d.sourceFilename = ?3 AND d.introducingChangeSet IS NULL")
    DatabaseChangeSet findIntroducingChangeSet(Project project, String internalId, String sourceFileName);

    /**
     * Deletes all change sets which belong to a given project.
     *
     * @param projectId
     *         Id of the project.
     */
    @Modifying
    @Query(value = "DELETE FROM CHANGESET WHERE REVISION_ID IN (SELECT ID FROM REVISION WHERE PROJECT_ID = ?1)", nativeQuery = true)
    void deleteAllByProjectId(Long projectId);
}
