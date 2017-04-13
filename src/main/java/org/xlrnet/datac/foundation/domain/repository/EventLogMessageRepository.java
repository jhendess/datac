package org.xlrnet.datac.foundation.domain.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.xlrnet.datac.foundation.domain.EventLogMessage;

/**
 * Repository for accessing event logs
 */
@Repository
public interface EventLogMessageRepository extends PagingAndSortingRepository<EventLogMessage, Long> {

}
