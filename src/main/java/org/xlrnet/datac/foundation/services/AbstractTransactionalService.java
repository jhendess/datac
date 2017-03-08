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
    @Transactional
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
    @Transactional(readOnly = true)
    public T findOne(Long id) {
        return crudRepository.findOne(id);
    }

    /**
     * Returns all entities in the repository.
     *
     * @return all entities.
     */
    @Transactional(readOnly = true)
    public Iterable<T> findAll() {
        return crudRepository.findAll();
    }

    /**
     * Saves a given entity. Use the returned instance for further operations as the save operation might have changed
     * the entity instance completely.
     *
     * @param entity
     *         The entity to save.
     * @return the saved entity
     */
    public <S extends T> S save(S entity) {
        return crudRepository.save(entity);
    }

    /**
     * Saves all given entities.
     *
     * @param entities
     *         The entities to save.
     * @return the saved entities.
     * @throws IllegalArgumentException
     *         in case the given entity is {@literal null}.
     */
    @Transactional
    public <S extends T> Iterable<S> save(Iterable<S> entities) {
        return crudRepository.save(entities);
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
