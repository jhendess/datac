package org.xlrnet.datac.vcs.domain.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.domain.Revision;

/**
 * Repository for accessing revision data.
 */
public interface RevisionRepository extends CrudRepository<Revision, Long> {

    @Transactional(readOnly = true)
    List<Revision> findAllByProject(Project project);

}
