package org.xlrnet.datac.database.services;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.LockFailedException;
import org.xlrnet.datac.commons.graph.BreadthFirstTraverser;
import org.xlrnet.datac.commons.util.SortableComparator;
import org.xlrnet.datac.database.domain.DatabaseChange;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.domain.repository.ChangeSetRepository;
import org.xlrnet.datac.foundation.configuration.async.ThreadScoped;
import org.xlrnet.datac.foundation.domain.*;
import org.xlrnet.datac.foundation.domain.validation.SortOrderValidator;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.foundation.services.EventLogService;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.LockingService;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Transactional service for accessing change set data. This service is thread-scoped in order to guarantee isolated
 * caches. FIXME: Maybe this isn't a good idea if threads are being reused?
 */
@Service
@ThreadScoped
public class ChangeSetService extends AbstractTransactionalService<DatabaseChangeSet, ChangeSetRepository> {

    private final Logger LOGGER = LoggerFactory.getLogger(ChangeSetService.class);

    private final SortOrderValidator sortOrderValidator;

    /**
     * Service for accessing the revision graph.
     */
    private final RevisionGraphService revisionGraphService;

    /**
     * Central locking service.
     */
    private final LockingService lockingService;

    /**
     * Event logging service.
     */
    private final EventLogService eventLogService;

    /**
     * Helper class for performing breadth first traversals on revision graphs.
     */
    private final BreadthFirstTraverser<Revision> breadthFirstTraverser = new BreadthFirstTraverser<>();

    /**
     * Cache for introducing change set ids.
     */
    private Map<MultiKey, Optional<Long>> introducingChangeSetIdCache = new HashMap<>();

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *
     * @param crudRepository
     *         The crud repository for providing basic crud operations.
     * @param lockingService
     * @param eventLogService
     */
    @Autowired
    public ChangeSetService(ChangeSetRepository crudRepository, SortOrderValidator sortOrderValidator, RevisionGraphService revisionGraphService, LockingService lockingService, EventLogService eventLogService) {
        super(crudRepository);
        this.sortOrderValidator = sortOrderValidator;
        this.revisionGraphService = revisionGraphService;
        this.lockingService = lockingService;
        this.eventLogService = eventLogService;
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
     * Finds the last database change sets on a given branch. This method will iterate via breadth-first traversal over
     * the VCS revisions in order to find the latest change sets.
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
    // TODO: Write tests for this method
    public List<DatabaseChangeSet> findLastDatabaseChangeSetsOnBranch(Branch branch, int changeSetsToFind, int revisionsToVisit) throws DatacTechnicalException {
        Project project = branch.getProject();
        Revision lastDevRevision = revisionGraphService.findByInternalIdAndProject(branch.getInternalId(), project);
        ArrayList<DatabaseChangeSet> changeSets = new ArrayList<>();
        AtomicInteger visitedRevisions = new AtomicInteger(0);
        breadthFirstTraverser.traverseParentsCutOnMatch(lastDevRevision, (r) -> {
            if (countByRevision(r) > 0) {
                List<DatabaseChangeSet> changeSetsInRevision = findAllInRevision(r);
                for (int i = changeSetsInRevision.size() - 1; i > 0 && changeSets.size() < changeSetsToFind; i--) {
                    changeSets.add(changeSetsInRevision.get(i));
                }
            }
        }, (r -> (visitedRevisions.incrementAndGet() > revisionsToVisit || changeSets.size() == 3)));
        return changeSets;
    }

    /**
     * Returns the last database change sets on the given branch. Traverses only the given amount of revisions before an
     * empty list will be returned. The resulting list begins with the oldest change set and ends with the newest. The
     * changes inside the change sets will be completely initialized.
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
            if (changeSetsInRevision.isEmpty() && countByRevision(r) > 0) {
                for (DatabaseChangeSet databaseChangeSet : findAllInRevision(r)) {
                    changeSetsInRevision.add(databaseChangeSet);
                    Hibernate.initialize(databaseChangeSet.getChanges());
                }
            }
        }, (r -> (visitedRevisions.incrementAndGet() > revisionsToVisit || !changeSetsInRevision.isEmpty())));
        return changeSetsInRevision;
    }

    /**
     * If the given database change set doesn't occur for the first time on the revision graph, link it with the change
     * set that introduced it. If the introducing change set has a different checksum, the given change set is marked as
     * modifying. Requires an actively running transaction.
     *
     * @param databaseChangeSet
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void linkRevisions(@NotNull DatabaseChangeSet databaseChangeSet) {
        checkState(!databaseChangeSet.isPersisted(), "The given change set may not be persisted");

        DatabaseChangeSet firstChangeSet = findIntroducingChangeSet(databaseChangeSet);
        if (firstChangeSet != null) {
            databaseChangeSet.setIntroducingChangeSet(firstChangeSet);
            if (!StringUtils.equals(databaseChangeSet.getChecksum(), firstChangeSet.getChecksum())) {
                databaseChangeSet.setModifying(true);
            }
        }
    }

    /**
     * Use a fallback algorithm to determine how a change should be displayed: <ol> <li>Try the actual comment</li>
     * <li>If the no comment is available, use the filename</li> <li>If there is no filename, use the SQL preview</li>
     * <li>If no SQL preview, use the change type</li> <li>If there is no change content, use the checksum</li> </ol>
     * <p>
     */
    @NotNull
    @Transactional(readOnly = true)
    public String formatDatabaseChangeSetTitle(@NotNull DatabaseChangeSet changeSet) {
        String message = changeSet.getComment();

        if (StringUtils.isBlank(message)) {
            message = StringUtils.substringAfterLast(changeSet.getSourceFilename().replace("\\", "/"), "/");

            if ("raw".equalsIgnoreCase(changeSet.getInternalId())) {
                if (StringUtils.isBlank(message)) {
                    DatabaseChangeSet refreshed = getRepository().findOne(changeSet.getId());
                    if (!refreshed.getChanges().isEmpty()) {
                        DatabaseChange firstChange = refreshed.getChanges().get(0);
                        if (StringUtils.isNotBlank(firstChange.getPreviewSql())) {
                            message = firstChange.getPreviewSql();
                        } else {
                            message = firstChange.getType();
                        }
                    } else {
                        message = refreshed.getChecksum();
                    }
                }
            } else {
                message += " (" + changeSet.getInternalId() + ")";
            }
        }

        if (message == null) {
            message = "";
        }

        return message;
    }

    private DatabaseChangeSet findIntroducingChangeSet(@NotNull DatabaseChangeSet databaseChangeSet) {
        DatabaseChangeSet introducingChangeSet;
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

    @Transactional
    public void resetChanges(Project project) throws DatacTechnicalException {
        if (!lockingService.tryLock(project)) {
            throw new LockFailedException(project);
        }
        EventLog eventLog = eventLogService.newEventLog().setProject(project).setType(EventType.CHANGESET_RESET);
        try {
            LOGGER.warn("Deleting all change sets in project {}", project.getName());
            eventLog.addMessage(new EventLogMessage("Resetting change sets").setSeverity(MessageSeverity.WARNING));
            getRepository().deleteAllByProjectId(project.getId());
            eventLogService.save(eventLog);
        } catch (RuntimeException e) {
            eventLogService.addExceptionToEventLog(eventLog, "Resetting change sets failed", e);
            eventLogService.save(eventLog);
            throw new DatacTechnicalException(e);
        } finally {
            lockingService.unlock(project);
        }
    }
}
