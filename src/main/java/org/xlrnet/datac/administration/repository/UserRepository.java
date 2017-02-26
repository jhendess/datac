package org.xlrnet.datac.administration.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.administration.domain.User;

import java.util.List;

/**
 * Repository for accessing user related data.
 */
public interface UserRepository extends CrudRepository<User, Long> {

    /**
     * Returns all users ordered by their login name in ascending order.
     *
     * @return all users ordered by their login name in ascending order.
     */
    @Transactional(readOnly = true)
    List<User> findAllByOrderByLoginNameAsc();

    /**
     * Returns the first user with a given login name.
     *
     * @return the first user with a given login name.
     */
    @Transactional(readOnly = true)
    User findFirstByLoginNameIgnoreCase(String loginName);
}
