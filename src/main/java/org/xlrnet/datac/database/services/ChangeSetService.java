package org.xlrnet.datac.database.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.graph.BreadthFirstTraverser;
import org.xlrnet.datac.commons.util.SortableComparator;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.domain.repository.ChangeSetRepository;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.validation.SortOrderValidator;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Transactional service for accessing change set data.
 */
@Service
public class ChangeSetService extends AbstractTransactionalService<DatabaseChangeSet, ChangeSetRepository> {

    private final SortOrderValidator sortOrderValidator;

    /**
     * Service for accessing the revision graph.
     */
    private final RevisionGraphService revisionGraphService;

    /**
     * Helper class for performing breadth first traversals on revision graphs.
     */
    private final BreadthFirstTraverser<Revision> breadthFirstTraverser = new BreadthFirstTraverser<>();

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *  @param crudRepository
     *         The crud repository for providing basic crud operations.
     */
    @Autowired
    public ChangeSetService(ChangeSetRepository crudRepository, SortOrderValidator sortOrderValidator, RevisionGraphService revisionGraphService) {
        super(crudRepository);
        this.sortOrderValidator = sortOrderValidator;
        this.revisionGraphService = revisionGraphService;
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

    /**
     * Finds the last database change sets on a given branch. This method will iterate via breadth-first traversal
     * over the VCS revisions in order to find the latest change sets.
     *
     * @param branch
     *         The branch on which should be searched.
     * @param changeSetsToFind
     *         The amount of change sets that should be returned.
     * @param revisionsToVisit
     *         The amount of revisions that should be visited until the search is given up.
     * @return
     * @throws DatacTechnicalException
     */
    public List<DatabaseChangeSet> findLastDatabaseChangeSetsOnBranch(Branch branch, int changeSetsToFind, int revisionsToVisit) throws DatacTechnicalException {
        Project project = branch.getProject();
        Revision lastDevRevision = revisionGraphService.findByInternalIdAndProject(branch.getInternalId(), project);
        ArrayList<DatabaseChangeSet> changeSets = new ArrayList<>();
        AtomicInteger visitedRevisions = new AtomicInteger(0);
        breadthFirstTraverser.traverseParentsCutOnMatch(lastDevRevision, (r) -> {
            if (countByRevision(r) > 0) {
                List<DatabaseChangeSet> changeSetsInRevision = findAllInRevision(r);
                for (int i = changeSetsInRevision.size() - 1, k = 0; i > 0 && k < changeSetsToFind; i--) {
                    changeSets.add(changeSetsInRevision.get(i));
                    k++;
                }
            }
        }, (r -> (visitedRevisions.incrementAndGet() > revisionsToVisit || changeSets.size() == 3)));
        return changeSets;
    }
}
