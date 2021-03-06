package org.xlrnet.datac.foundation.domain.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.xlrnet.datac.foundation.domain.EventLog;

/**
 * Repository for accessing event logs
 */
@Repository
public interface EventLogRepository extends PagingAndSortingRepository<EventLog, Long> {

}
