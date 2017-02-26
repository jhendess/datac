package org.xlrnet.datac.foundation.services;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract base class for all services which perform transactional operations.
 */
@Transactional
public class AbstractTransactionalService<T, R extends CrudRepository<T, Long>> {

    // TODO: Discuss if it makes in most cases sense to have a separate layer above CrudRepository (Spring Data API seems to be a viable alternative for custom queries)

    /**
     * The concrete {@link CrudRepository} for this service.
     */
    private final R crudRepository;

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *
     * @param crudRepository
     *         The crud repository for providing basic crud operations.
     */
    public AbstractTransactionalService(R crudRepository) {
        this.crudRepository = crudRepository;
    }

    /**
     * Deletes the given entity from database.
     *
     * @param entity
     *         The entity to delete.
     */
    public void delete(T entity) {
        crudRepository.delete(entity);
    }

    /**
     * Returns the entity with a given id.
     *
     * @param id
     *         The id to find.
     * @return the entity with a given id.
     */
    public T findOne(Long id) {
        return crudRepository.findOne(id);
    }

    /**
     * Returns all entities in the repository.
     *
     * @return all entities.
     */
    public Iterable<T> findAll() {
        return crudRepository.findAll();
    }

    /**
     * Returns the crud service for this service.
     *
     * @return the crud service for this service.
     */
    protected R getRepository() {
        return crudRepository;
    }
}
