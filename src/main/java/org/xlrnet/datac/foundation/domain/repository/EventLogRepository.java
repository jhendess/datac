package org.xlrnet.datac.foundation.domain.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.xlrnet.datac.foundation.domain.EventLog;

/**
 * Repository for accessing event logs
 */
@Repository
public interface EventLogRepository extends CrudRepository<EventLog, Long> {

}
