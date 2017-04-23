package org.xlrnet.datac.database.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.commons.util.SortableComparator;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.domain.repository.ChangeSetRepository;
import org.xlrnet.datac.foundation.domain.validation.SortOrderValidator;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.vcs.domain.Revision;

import java.util.Collection;
import java.util.List;

/**
 * Transactional service for accessing change set data.
 */
@Service
public class ChangeSetService extends AbstractTransactionalService<DatabaseChangeSet, ChangeSetRepository> {

    private final SortOrderValidator sortOrderValidator;

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *
     * @param crudRepository
     *         The crud repository for providing basic crud operations.
     * @param sortOrderValidator
     *         Validator for the sort order.
     */
    @Autowired
    public ChangeSetService(ChangeSetRepository crudRepository, SortOrderValidator sortOrderValidator) {
        super(crudRepository);
        this.sortOrderValidator = sortOrderValidator;
    }

    /**
     * Returns a list of change sets in the given revision. The list is ordered ascending by the sort order defined in
     * {@link DatabaseChangeSet#getSort()}
     *
     * @param revision
     *         The revision in which the change sets must lie.
     * @return A list of change sets in the given revision.
     */
    public List<DatabaseChangeSet> findAllInRevision(Revision revision) {
        List<DatabaseChangeSet> allByRevision = getRepository().findAllByRevision(revision);
        allByRevision.sort(new SortableComparator());
        return allByRevision;
    }

    public <S extends DatabaseChangeSet> Collection<S> save(Collection<S> entities) {
        sortOrderValidator.isValid(entities, null);
        return (Collection<S>) super.save(entities);
    }

    /**
     * Counts the change sets for a given revision.
     *
     * @param revision
     *         Revision for which the change sets should be counted.
     * @return Number of change sets in the given revision.
     */
    public long countByRevision(Revision revision) {
        return getRepository().countAllByRevision(revision);
    }
}
