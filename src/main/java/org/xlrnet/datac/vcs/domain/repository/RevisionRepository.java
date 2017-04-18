package org.xlrnet.datac.vcs.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.domain.Revision;

import java.util.List;

/**
 * Repository for accessing revision data.
 */
public interface RevisionRepository extends PagingAndSortingRepository<Revision, Long> {

    @Transactional(readOnly = true)
    List<Revision> findAllByProject(Project project);

    @Transactional(readOnly = true)
    Revision findByInternalIdAndProject(String revisionId, Project project);

    @Transactional(readOnly = true)
    long countRevisionByInternalIdAndProject(String revisionId, Project project);

    @Transactional(readOnly = true)
    @Query(value = "SELECT * FROM REVISION LEFT JOIN REVISION_GRAPH ON REVISION.ID = REVISION_GRAPH.REVISION_ID WHERE REVISION.PROJECT_ID = ?1 AND PARENT_REVISION_ID IS NULL", nativeQuery = true)
    Revision findProjectRootRevision(long projectId);
}
