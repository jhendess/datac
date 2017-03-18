package org.xlrnet.datac.vcs.domain.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.xlrnet.datac.vcs.domain.Revision;

/**
 * Repository for accessing revision data.
 */
@Repository
public interface RevisionRepository extends CrudRepository<Revision, Long> {

}
