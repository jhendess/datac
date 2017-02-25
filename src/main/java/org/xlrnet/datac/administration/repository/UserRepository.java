package org.xlrnet.datac.administration.repository;

import org.springframework.data.repository.CrudRepository;
import org.xlrnet.datac.administration.domain.User;

import java.util.List;

/**
 * Repository for accessing user related data.
 */
public interface UserRepository extends CrudRepository<User, Long> {

    /**
     * Returns all users ordered by their login name in ascending order.
     */
    List<User> findAllByOrderByLoginNameAsc();
}
