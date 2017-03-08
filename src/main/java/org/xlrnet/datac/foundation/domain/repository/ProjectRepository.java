package org.xlrnet.datac.foundation.domain.repository;

import org.springframework.data.repository.CrudRepository;
import org.xlrnet.datac.foundation.domain.Project;

/**
 * Repository for accessing project data.
 */
public interface ProjectRepository extends CrudRepository<Project, Long> {

}
