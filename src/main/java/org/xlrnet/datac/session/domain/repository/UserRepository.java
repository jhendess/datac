package org.xlrnet.datac.session.domain.repository;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.session.domain.User;

/**
 * Repository for accessing user related data.
 */
public interface UserRepository extends PagingAndSortingRepository<User, Long> {

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
