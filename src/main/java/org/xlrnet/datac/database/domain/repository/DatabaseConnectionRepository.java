package org.xlrnet.datac.database.domain.repository;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.database.domain.DatabaseConnection;

/**
 * Repository for database connection configuration data.
 */
public interface DatabaseConnectionRepository extends PagingAndSortingRepository<DatabaseConnection, Long> {

    /**
     * Returns all connections ordered by their name in ascending order.
     *
     * @return all connections ordered by their name in ascending order.
     */
    @Transactional(readOnly = true)
    List<DatabaseConnection> findAllByOrderByName();
}
