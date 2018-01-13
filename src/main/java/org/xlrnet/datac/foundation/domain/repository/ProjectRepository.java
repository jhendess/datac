package org.xlrnet.datac.foundation.domain.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.ProjectState;

import java.util.Collection;

/**
 * Repository for accessing project data.
 */
public interface ProjectRepository extends PagingAndSortingRepository<Project, Long> {

    Collection<Project> findAllByStateIn(Iterable<ProjectState> states);
}
