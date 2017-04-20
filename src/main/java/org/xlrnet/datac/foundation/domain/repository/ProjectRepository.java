package org.xlrnet.datac.foundation.domain.repository;

import java.util.Collection;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.ProjectState;

/**
 * Repository for accessing project data.
 */
public interface ProjectRepository extends PagingAndSortingRepository<Project, Long> {

    Collection<Project> findAllByStateIn(Iterable<ProjectState> states);
}
