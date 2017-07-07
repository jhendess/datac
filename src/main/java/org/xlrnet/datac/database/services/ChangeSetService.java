package org.xlrnet.datac.database.services;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.graph.BreadthFirstTraverser;
import org.xlrnet.datac.commons.util.SortableComparator;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.domain.repository.ChangeSetRepository;
import org.xlrnet.datac.foundation.configuration.async.ThreadScoped;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.validation.SortOrderValidator;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

/**
 * Transactional service for accessing change set data. This service is thread-scoped in order to guarantee isolated
 * caches. FIXME: Maybe this isn't a good idea if threads are being reused?
 */
@Service
@ThreadScoped
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

    private Map<MultiKey, Optional<Long>> introducingChangeSetIdCache = new HashMap<>();

    private Map<MultiKey, Optional<Collection<Long>>> overwrittenChangeSetIdCache = new HashMap<>();

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

    /**
     * Returns the last database change sets on the given branch. Traverses only the given amount of revisions before an empty list will be returned.
     *
     * @param branch
     *         The branch on which should be searched.
     * @param revisionsToVisit
     *         The amount of revisions that should be visited until the search is given up.
     */
    public List<DatabaseChangeSet> findLastDatabaseChangeSetsOnBranch(Branch branch, int revisionsToVisit) throws DatacTechnicalException {
        Revision lastDevRevision = revisionGraphService.findByInternalIdAndProject(branch.getInternalId(), branch.getProject());
        final List<DatabaseChangeSet> changeSetsInRevision = new ArrayList<>();
        AtomicInteger visitedRevisions = new AtomicInteger(0);
        breadthFirstTraverser.traverseParentsCutOnMatch(lastDevRevision, (r) -> {
            if (countByRevision(r) > 0) {
                changeSetsInRevision.addAll(findAllInRevision(r));
            }
        }, (r -> (visitedRevisions.incrementAndGet() > revisionsToVisit || !changeSetsInRevision.isEmpty())));
        return changeSetsInRevision;
    }

    /**
     * Adds the following links to the given {@link DatabaseChangeSet}:
     * <ul>
     *     <li>The revision where this change set was first introduced</li>
     *     <li>The revision which is overwritten by this change set</li>
     * </ul>
     * Furthermore, if the given change set overwrites a change set, the overwritten change set will be updated, too. Requires an actively running transaction.
     * @param databaseChangeSet
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void linkRevisions(@NotNull DatabaseChangeSet databaseChangeSet) {
        checkState(!databaseChangeSet.isPersisted(), "The given change set may not be persisted");
        Project project = databaseChangeSet.getRevision().getProject();

        DatabaseChangeSet firstChangeSet = findIntroducingChangeSet(databaseChangeSet);
        if (firstChangeSet != null) {
            databaseChangeSet.setIntroducingChangeSet(firstChangeSet);
        }

        Collection<DatabaseChangeSet> overwrittenChangeSets = findOverwrittenChangeSets(databaseChangeSet);

        for (DatabaseChangeSet overwrittenChangeSet : overwrittenChangeSets) {
            overwrittenChangeSet.setConflictingChangeSet(databaseChangeSet);
            databaseChangeSet.setOverwrittenChangeSet(overwrittenChangeSet);
            save(overwrittenChangeSet);
        }
    }

    private DatabaseChangeSet findIntroducingChangeSet(@NotNull DatabaseChangeSet databaseChangeSet) {
        DatabaseChangeSet introducingChangeSet = null;
        Project project = databaseChangeSet.getRevision().getProject();

        MultiKey introducingChangeKey = new MultiKey(project.getId(), databaseChangeSet.getInternalId(), databaseChangeSet.getSourceFilename());
        if (introducingChangeSetIdCache.containsKey(introducingChangeKey) && introducingChangeSetIdCache.get(introducingChangeKey).isPresent()) {
            // If the entry is not present, there is no overwriting change
            introducingChangeSet = getRepository().findOne(introducingChangeSetIdCache.get(introducingChangeKey).get());
        } else {
            introducingChangeSet = getRepository().findIntroducingChangeSet(project, databaseChangeSet.getInternalId(), databaseChangeSet.getSourceFilename());
            if (introducingChangeSet == null) {
                introducingChangeSetIdCache.put(introducingChangeKey, Optional.empty());
            } else {
                introducingChangeSetIdCache.put(introducingChangeKey, Optional.of(introducingChangeSet.getId()));
            }
        }
        
        return introducingChangeSet;
    }

    private Collection<DatabaseChangeSet> findOverwrittenChangeSets(@NotNull DatabaseChangeSet databaseChangeSet) {
        Collection<DatabaseChangeSet> overwrittenChangeSets = new ArrayList<>();
        Project project = databaseChangeSet.getRevision().getProject();

        MultiKey overwrittenChangeKey = new MultiKey(project.getId(), databaseChangeSet.getInternalId(), databaseChangeSet.getSourceFilename(), databaseChangeSet.getChecksum());
        if (overwrittenChangeSetIdCache.containsKey(overwrittenChangeKey)) {
            // If the entry is not present, there is no overwriting change
            if (overwrittenChangeSetIdCache.get(overwrittenChangeKey).isPresent()) {
                getRepository().findAll(overwrittenChangeSetIdCache.get(overwrittenChangeKey).get());
            }
        } else {
            overwrittenChangeSets = getRepository().findOverwrittenChangeSets(project, databaseChangeSet.getInternalId(), databaseChangeSet.getSourceFilename(), databaseChangeSet.getChecksum());
            if (overwrittenChangeSets == null) {
                overwrittenChangeSetIdCache.put(overwrittenChangeKey, Optional.empty());
            } else {
                overwrittenChangeSetIdCache.put(overwrittenChangeKey, Optional.of(overwrittenChangeSets.stream().map(DatabaseChangeSet::getId).collect(Collectors.toList())));
            }
        }
        return overwrittenChangeSets;
    }
}
