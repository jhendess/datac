package org.xlrnet.datac.database.domain.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.domain.Revision;

import java.util.List;

/**
 * Repository for accessing change set data.
 */
public interface ChangeSetRepository extends PagingAndSortingRepository<DatabaseChangeSet, Long> {

    List<DatabaseChangeSet> findAllByRevision(Revision revision);

    long countAllByRevision(Revision revision);

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
