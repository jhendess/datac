package org.xlrnet.datac.vcs.domain.repository;

import java.math.BigInteger;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.domain.Revision;

/**
 * Repository for accessing revision data.
 */
public interface RevisionRepository extends PagingAndSortingRepository<Revision, Long> {

    @Transactional(readOnly = true)
    List<Revision> findAllByProject(Project project);

    @Transactional(readOnly = true)
    List<Revision> findAllByProject(Project project, Pageable pageable);

    @Transactional(readOnly = true)
    Revision findByInternalIdAndProject(String revisionId, Project project);

    @Transactional(readOnly = true)
    long countRevisionByInternalIdAndProject(String revisionId, Project project);

    @Transactional(readOnly = true)
    @Query(value = "SELECT P.INTERNAL_ID AS PARENT_ID, C.INTERNAL_ID AS CHILD_ID FROM REVISION P JOIN REVISION_GRAPH G on P.ID = G.PARENT_REVISION_ID JOIN REVISION C ON G.REVISION_ID = C.ID WHERE P.PROJECT_ID = ?1", nativeQuery = true)
    List<Object[]> findAllParentChildRelationsInProject(long projectId);

    @Transactional(readOnly = true)
    @Query(value = "SELECT * FROM REVISION LEFT JOIN REVISION_GRAPH ON REVISION.ID = REVISION_GRAPH.REVISION_ID WHERE REVISION.PROJECT_ID = ?1 AND PARENT_REVISION_ID IS NULL", nativeQuery = true)
    Revision findProjectRootRevision(long projectId);

    /**
     * Returns a list of all revision ids which are a merge revision.
     */
    @Transactional(readOnly = true)
    @Query(value = "SELECT REVISION_ID FROM (\n" +
            "  SELECT\n" +
            "    REVISION_ID,\n" +
            "    COUNT(REVISION_ID) AS REV_COUNT\n" +
            "  FROM REVISION\n" +
            "    LEFT JOIN REVISION_GRAPH ON REVISION.ID = REVISION_GRAPH.REVISION_ID\n" +
            "  WHERE REVISION.PROJECT_ID = ?1\n" +
            "  GROUP BY REVISION_ID\n" +
            ") WHERE REV_COUNT > 1", nativeQuery = true)
    List<BigInteger> findMergeRevisionIdsInProject(long projectId);

    @Transactional(readOnly = true)
    @Query(nativeQuery = true, value = "SELECT DISTINCT R.* FROM REVISION R JOIN CHANGESET C ON R.ID = C.REVISION_ID AND R.PROJECT_ID = ?1")
    List<Revision> findAllWithModifyingDatabaseChangesInProject(long projectId);
}
