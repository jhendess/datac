package org.xlrnet.datac.vcs.domain.repository;

import org.springframework.data.repository.CrudRepository;
import org.xlrnet.datac.vcs.domain.Branch;

/**
 * Repository for accessing branch data.
 */
public interface BranchRepository extends CrudRepository<Branch, Long> {
}