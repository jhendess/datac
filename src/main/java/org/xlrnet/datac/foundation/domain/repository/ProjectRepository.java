package org.xlrnet.datac.foundation.domain.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.xlrnet.datac.foundation.domain.Project;

/**
 * Repository for accessing project data.
 */
public interface ProjectRepository extends PagingAndSortingRepository<Project, Long> {

}
