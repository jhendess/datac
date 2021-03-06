package org.xlrnet.datac.database.services;

import static com.google.common.base.Preconditions.checkState;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.LockFailedException;
import org.xlrnet.datac.commons.graph.BreadthFirstTraverser;
import org.xlrnet.datac.commons.util.SortableComparator;
import org.xlrnet.datac.commons.util.TechnicalRuntimeException;
import org.xlrnet.datac.database.domain.DatabaseChange;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.domain.repository.ChangeSetRepository;
import org.xlrnet.datac.foundation.domain.EventLog;
import org.xlrnet.datac.foundation.domain.EventLogMessage;
import org.xlrnet.datac.foundation.domain.EventType;
import org.xlrnet.datac.foundation.domain.MessageSeverity;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.validation.SortOrderValidator;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.foundation.services.EventLogService;
import org.xlrnet.datac.foundation.services.ProjectCacheReloadEvent;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.LockingService;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

/**
 * Transactional service for accessing change set data. This service is thread-scoped in order to guarantee isolated
 * caches. FIXME: Maybe this isn't a good idea if threads are being reused?
 */
@Service
//@ThreadScoped
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
     * Application event publisher.
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Cache for number of change sets per revision per project.
     */
    private Map<Long, Map<Long, Long>> changeSetCountByProjectCache = new HashMap<>();

    private BasicFormatterImpl changeSetFormatter = new BasicFormatterImpl();

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *  @param crudRepository
     *         The crud repository for providing basic crud operations.
     * @param lockingService
     * @param eventLogService
     * @param eventPublisher
     */
    @Autowired
    public ChangeSetService(ChangeSetRepository crudRepository, SortOrderValidator sortOrderValidator, RevisionGraphService revisionGraphService, LockingService lockingService, EventLogService eventLogService, ApplicationEventPublisher eventPublisher) {
        super(crudRepository);
        this.sortOrderValidator = sortOrderValidator;
        this.revisionGraphService = revisionGraphService;
        this.lockingService = lockingService;
        this.eventLogService = eventLogService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Counts all change sets which were modified the given change set.
     *
     * @param changeSet
     *         The change set to change for overwrites.
     * @return The number of change sets conflicting with the given changeset.
     */
    public long countModifyingChangeSets(DatabaseChangeSet changeSet) {
        return getRepository().countModifyingChangeSets(changeSet);
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
        Revision refreshed = revisionGraphService.refresh(revision);
        List<DatabaseChangeSet> allByRevision = getRepository().findAllByRevision(refreshed);
        allByRevision.sort(new SortableComparator());
        return allByRevision;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String formatPreviewSql(DatabaseChangeSet changeSet) {
        return changeSetFormatter.format(changeSet.getChanges().get(0).getPreviewSql());
    }

    public <S extends DatabaseChangeSet> Collection<S> save(Collection<S> entities) {
        if (sortOrderValidator.isValid(entities, null)) {
            return (Collection<S>) super.save(entities);
        } else {
            throw new TechnicalRuntimeException("Change sets are not uniquely sorted");
        }
    }

    /**
     * Counts the change sets for a given revision.
     *
     * @param revision
     *         Revision for which the change sets should be counted.
     * @return Number of change sets in the given revision.
     */
    public long countByRevision(Revision revision) {
        return getRepository().countByRevisionId(revision.getId());
    }

    /**
     * Counts the change sets for a given revision using an internal cache.
     *
     * @param revision
     *         Revision for which the change sets should be counted.
     * @return Number of change sets in the given revision.
     */
    public long countCachedByRevision(Revision revision) {
        Map<Long, Long> countCache = getCountCacheByProject(revision.getProject());
        return countCache.getOrDefault(revision.getId(), 0L);
    }

    @EventListener
    public void forceProjectCacheReload(ProjectCacheReloadEvent event) {
        reloadCountCache(event.getProject());
    }

    private Map<Long, Long> getCountCacheByProject(Project project) {
        Long projectId = project.getId();
        if (!changeSetCountByProjectCache.containsKey(projectId)) {
            reloadCountCache(project);
        }
        return changeSetCountByProjectCache.get(projectId);
    }

    private void reloadCountCache(Project project) {
        LOGGER.debug("Updating change set count cache for project {}", project.getName());
        Stream<Object[]> countAllByProject = getRepository().countAllByProject(project.getId());
        Map<Long, Long> newCache = new HashMap<>();

        countAllByProject.forEach(v -> newCache.put(((BigInteger)v[0]).longValueExact(), ((BigInteger)v[1]).longValueExact()));
        changeSetCountByProjectCache.put(project.getId(), newCache);
        LOGGER.debug("Finished loading change set count cache for project {}", project.getName());
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
    @Transactional(readOnly = true)
    public List<DatabaseChangeSet> findLastDatabaseChangeSetsOnBranch(Branch branch, int changeSetsToFind, int revisionsToVisit) throws DatacTechnicalException {
        Project project = branch.getProject();
        Revision lastDevRevision = revisionGraphService.findCachedByInternalIdAndProject(branch.getInternalId(), project);
        ArrayList<DatabaseChangeSet> changeSets = new ArrayList<>();
        AtomicInteger visitedRevisions = new AtomicInteger(0);
        breadthFirstTraverser.traverseParentsCutOnMatch(lastDevRevision, (r) -> {
            long countByRevision = countCachedByRevision(r);
            if (countByRevision > 0) {
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
    @Transactional(readOnly = true)
    public List<DatabaseChangeSet> findLastDatabaseChangeSetsOnBranch(@NotNull Branch branch, int revisionsToVisit) throws DatacTechnicalException {
        Revision lastDevRevision = revisionGraphService.findByInternalIdAndProject(branch.getInternalId(), branch.getProject());
        return findDatabaseChangeSetsInRevision(lastDevRevision, revisionsToVisit);
    }

    /**
     * Returns the last database change sets in the given revision. Traverses only the given amount of revisions before an
     * empty list will be returned. The resulting list begins with the oldest change set and ends with the newest. The
     * changes inside the change sets will be completely initialized.
     *
     * @param revision
     *         The revision in which should be searched.
     * @param revisionsToVisit
     *         The amount of revisions that should be visited until the search is given up.
     */
    @NotNull
    @Transactional(readOnly = true)
    public List<DatabaseChangeSet> findDatabaseChangeSetsInRevision(@NotNull Revision revision, int revisionsToVisit) throws DatacTechnicalException {
        Revision reloadedRevision = revisionGraphService.findCachedByInternalIdAndProject(revision.getInternalId(), revision.getProject());
        final List<DatabaseChangeSet> changeSetsInRevision = new ArrayList<>();
        AtomicInteger visitedRevisions = new AtomicInteger(0);
        breadthFirstTraverser.traverseParentsCutOnMatch(reloadedRevision, (r) -> {
            LOGGER.trace("Checking revision {} for change sets", r.getInternalId());
            if (changeSetsInRevision.isEmpty() && countCachedByRevision(r) > 0) {
                LOGGER.trace("Loading all change sets in revision {}", r.getInternalId());
                for (DatabaseChangeSet databaseChangeSet : findAllInRevision(r)) {
                    changeSetsInRevision.add(databaseChangeSet);
                    LOGGER.trace("Initializing changes for change set {}", databaseChangeSet.getId());
                    Hibernate.initialize(databaseChangeSet.getChanges());
                }
            }
        }, (r -> (visitedRevisions.incrementAndGet() > revisionsToVisit || !changeSetsInRevision.isEmpty())));
        return changeSetsInRevision;
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

        message = StringUtils.trimToEmpty(message);

        return message;
    }

    private DatabaseChangeSet findIntroducingChangeSet(@NotNull DatabaseChangeSet databaseChangeSet, Map<MultiKey, Optional<Long>> introducingChangeSetIdCache) {
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

    /**
     * Deletes all changes and change sets associated with the given project. The project may not be locked for this to operate correctly.
     *
     * @param project
     * @throws DatacTechnicalException
     */
    public void resetChanges(Project project) throws DatacTechnicalException {
        if (!lockingService.tryLock(project)) {
            throw new LockFailedException(project);
        }
        EventLog eventLog = eventLogService.newEventLog().setProject(project).setType(EventType.CHANGESET_RESET);
        try {
            LOGGER.warn("Deleting all change sets in project {}", project.getName());
            eventLog.addMessage(new EventLogMessage("Resetting change sets").setSeverity(MessageSeverity.WARNING));
            getRepository().deleteAllByProjectId(project.getId());
            eventPublisher.publishEvent(new ProjectCacheReloadEvent(this, project));
            eventLogService.save(eventLog);
        } catch (RuntimeException e) {
            eventLogService.addExceptionToEventLog(eventLog, "Resetting change sets failed", e);
            eventLogService.save(eventLog);
            throw new DatacTechnicalException(e);
        } finally {
            lockingService.unlock(project);
        }
    }

    /**
     * Link the given database change sets with their first and current revision and save them afterwards to the database.
     * @param databaseChangeSets The change sets to link and persist.
     * @param revision           The current revision.
     * @return
     */
    @Transactional
    public List<DatabaseChangeSet> linkRevisionsAndSave(List<DatabaseChangeSet> databaseChangeSets, Revision revision) {
        LOGGER.trace("Linking change sets to revisions");
        for (DatabaseChangeSet databaseChangeSet : databaseChangeSets) {
            Revision databaseRevision = revisionGraphService.findByInternalIdAndProject(revision.getInternalId(), revision.getProject());
            databaseChangeSet.setRevision(databaseRevision);
            linkRevisions(databaseChangeSet);
        }

        try {
            return (List<DatabaseChangeSet>) save(databaseChangeSets);
        } catch (DataIntegrityViolationException e) {
            LOGGER.error("Saving object failed." );
            throw new DatacRuntimeException(e);
        }
    }

    /**
     * If the given database change set doesn't occur for the first time on the revision graph, link it with the change
     * set that introduced it. If the introducing change set has a different checksum, the given change set is marked as
     * modifying. Requires an actively running transaction.
     *
     * @param databaseChangeSet
     */
    private void linkRevisions(@NotNull DatabaseChangeSet databaseChangeSet) {
        checkState(!databaseChangeSet.isPersisted(), "The given change set may not be persisted");

        Map<MultiKey, Optional<Long>> introducingChangeSetIdCache = new HashMap<>();
        DatabaseChangeSet firstChangeSet = findIntroducingChangeSet(databaseChangeSet, introducingChangeSetIdCache);
        if (firstChangeSet != null) {
            databaseChangeSet.setIntroducingChangeSet(firstChangeSet);
            if (!StringUtils.equals(databaseChangeSet.getChecksum(), firstChangeSet.getChecksum())) {
                databaseChangeSet.setModifying(true);
            }
        }
    }
}
