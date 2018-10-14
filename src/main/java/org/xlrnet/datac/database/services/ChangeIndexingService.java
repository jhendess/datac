package org.xlrnet.datac.database.services;

import static com.google.common.base.Preconditions.checkState;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.MissingDatabaseChangeSystemAdapterException;
import org.xlrnet.datac.commons.graph.BreadthFirstTraverser;
import org.xlrnet.datac.commons.graph.DepthFirstTraverser;
import org.xlrnet.datac.commons.util.SortableComparator;
import org.xlrnet.datac.database.api.DatabaseChangeSystemAdapter;
import org.xlrnet.datac.database.domain.DatabaseChange;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.foundation.components.EventLogProxy;
import org.xlrnet.datac.foundation.domain.EventLogMessage;
import org.xlrnet.datac.foundation.domain.MessageSeverity;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.ProjectState;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.vcs.api.VcsLocalRepository;
import org.xlrnet.datac.vcs.api.VcsRevision;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

/**
 * Service responsible for indexing database changes in a project.
 */
@Service
public class ChangeIndexingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeIndexingService.class);

    /**
     * Transaction timeout for indexing is increased to avoid errors.
     */
    private static final int INDEX_TRANSACTION_TIMEOUT = 3600;

    /**
     * Thread-scoped event log proxy.
     */
    private final EventLogProxy eventLog;

    /**
     * Service for accessing and parsing changelog files.
     */
    private final DatabaseChangeSystemAdapterRegistry databaseChangeSystemAdapterRegistry;

    /**
     * Service for accessing change sets.
     */
    private final ChangeSetService changeSetService;

    /**
     * Transactional service for project data.
     */
    private final ProjectService projectService;

    /**
     * Service for accessing the revision graph.
     */
    private final RevisionGraphService revisionGraphService;

    /**
     * Helper class for performing breadth first traversals on revision graphs.
     */
    private BreadthFirstTraverser<Revision> breadthFirstTraverser = new BreadthFirstTraverser<>();

    /**
     * Helper class for performing depth first traversals on revision graphs.
     */
    private DepthFirstTraverser<Revision> depthFirstTraverser = new DepthFirstTraverser<>();

    public ChangeIndexingService(EventLogProxy eventLog, DatabaseChangeSystemAdapterRegistry databaseChangeSystemAdapterRegistry, ChangeSetService changeSetService, ProjectService projectService, RevisionGraphService revisionGraphService) {
        this.eventLog = eventLog;
        this.databaseChangeSystemAdapterRegistry = databaseChangeSystemAdapterRegistry;
        this.changeSetService = changeSetService;
        this.projectService = projectService;
        this.revisionGraphService = revisionGraphService;
    }


    //@Transactional(timeout = INDEX_TRANSACTION_TIMEOUT)
    public Project indexDatabaseChanges(@NotNull Project project, @NotNull VcsLocalRepository localRepository) throws DatacTechnicalException {
        LOGGER.info("Begin indexing changes in project {}", project.getName());

        // Find revisions where only the changelog file itself changed to check if the given changelog file exists
        Collection<VcsRevision> changeLogRevisions = localRepository.listRevisionsWithChangesInPath(project.getChangelogLocation());

        if (changeLogRevisions.isEmpty()) {
            String msg = String.format("Couldn't find change log file %s for project %s", project.getChangelogLocation(), project.getName());
            eventLog.addMessage(new EventLogMessage(msg).setSeverity(MessageSeverity.WARNING));
            project.setState(ProjectState.MISSING_LOG);
            LOGGER.warn(msg);
            return project;
        }

        // For performing the actual indexing, retrieve all revisions which changed the whole directory in which the changelog lies
        Path parentPath = Paths.get(project.getChangelogLocation()).getParent();
        if (parentPath != null) {
            changeLogRevisions = localRepository.listRevisionsWithChangesInPath(parentPath.toString());
        } else {
            LOGGER.warn("Parent directory of change log may not be null - this is probably a bug in the VCS adapter. Falling back to direct file changes");
        }

        project.setState(ProjectState.INDEXING);
        Project updatedProject = projectService.save(project);
        projectService.saveAndPublishStateChange(updatedProject, 0);

        // Convert the external revisions to internal ones
        Collection<Revision> internalRevisions = revisionGraphService.findMatchingInternalRevisions(updatedProject, changeLogRevisions);
        // Find those revisions which don't have a change set yet
        Set<Revision> revisionsToIndex = new LinkedHashSet<>();
        for (Revision revision : internalRevisions) {
            if (changeSetService.countCachedByRevision(revision) == 0) {
                revisionsToIndex.add(revision);
            }
        }

        if (!revisionsToIndex.isEmpty()) {
            Set<Revision> orderedRevisionsToIndex = orderRevisionsToIndex(project, revisionsToIndex);

            // Add merge revisions after the regular revisions -> this works fine, since a merge revision is never an introducing change
            // The java Set makes sure that no revisions are indexed twice
            Iterable<Revision> mergeRevisionsInProject = revisionGraphService.findMergeRevisionsInProject(project);
            LOGGER.debug("Checking for merge revisions in project {} [id={}]", updatedProject.getName(), updatedProject.getId());
            for (Revision revision : mergeRevisionsInProject) {
                if (changeSetService.countCachedByRevision(revision) == 0 && localRepository.existsPathInRevision(revision, project.getChangelogLocation())) {
                    orderedRevisionsToIndex.add(revision);
                    LOGGER.debug("Adding merge revision {} to index", revision.getInternalId());
                }
            }

            LOGGER.debug("Indexing {} revisions in project {} [id={}]", orderedRevisionsToIndex.size(), updatedProject.getName(), updatedProject.getId());
            indexDatabaseChanges(updatedProject, localRepository, orderedRevisionsToIndex);
        } else {
            LOGGER.info("No new revisions in project {} [id={}]", updatedProject.getName(), updatedProject.getId());
        }

        return updatedProject;
    }

    @Transactional
    public void recalculateChecksumsInRevision(@NotNull Project project, @NotNull Revision revision, @NotNull VcsLocalRepository localRepository) throws DatacTechnicalException {
        localRepository.checkoutRevision(revision);
        LOGGER.debug("Recalculating checksums in revision {}", revision.getInternalId());
        List<DatabaseChangeSet> newChangeSets = listDatabaseChangeSetsInRevision(project, localRepository, revision);
        newChangeSets.sort(new SortableComparator());
        List<DatabaseChangeSet> oldChangeSets = changeSetService.findAllInRevision(revision);

        checkState(oldChangeSets.size() == newChangeSets.size(), "Number of change sets doesn't match in revision %s of project %s: expected %d, but got %d change sets", revision.getInternalId(), project.getName(), oldChangeSets.size(), newChangeSets.size());

        for (int i = 0; i < newChangeSets.size(); i++) {
            DatabaseChangeSet newChangeSet = newChangeSets.get(i);
            DatabaseChangeSet oldChangeSet = oldChangeSets.get(i);

            // Perform various validations to make sure we update the correct checksum and that the DCS didn't change their implementation
            checkState(StringUtils.equals(newChangeSet.getInternalId(), oldChangeSet.getInternalId()), "Internal id doesn't match in change set %s (file: %s) of revision %s of project %s: expected %s, but got %s", newChangeSet.getInternalId(), newChangeSet.getSourceFilename(), revision.getInternalId(), project.getName(), oldChangeSet.getInternalId(), newChangeSet.getInternalId());
            checkState(StringUtils.equals(newChangeSet.getSourceFilename(), oldChangeSet.getSourceFilename()), "Source filename doesn't match in change set %s (file: %s) of revision %s of project %s: expected %s, but got %s", newChangeSet.getInternalId(), newChangeSet.getSourceFilename(), revision.getInternalId(), project.getName(), oldChangeSet.getSourceFilename(), newChangeSet.getSourceFilename());
            checkState(newChangeSet.getChanges().size() == oldChangeSet.getChanges().size(), "Number of changes doesn't match in change set %s (file: %s) of revision %s of project %s: expected %d, but got %d changes", newChangeSet.getInternalId(), newChangeSet.getSourceFilename(), revision.getInternalId(), project.getName(), oldChangeSet.getChanges().size(), newChangeSet.getChanges().size());

            for (int j = 0; j < newChangeSet.getChanges().size(); j++) {
                DatabaseChange newChange = newChangeSet.getChanges().get(j);
                DatabaseChange oldChange = oldChangeSet.getChanges().get(j);

                // Perform various validations to make sure we update the correct checksum and that the DCS didn't change their implementation
                checkState(StringUtils.equals(newChange.getType(), oldChange.getType()), "Type doesn't match in change %d of change set %s (file: %s) of revision %s of project %s: expected %s, but got %s", newChange.getSort(), newChangeSet.getInternalId(), newChangeSet.getSourceFilename(), revision.getInternalId(), project.getName(), oldChange.getType(), newChange.getType());
                checkState(StringUtils.equals(newChange.getDescription(), oldChange.getDescription()), "Description doesn't match in change %d of change set %s (file: %s) of revision %s of project %s: expected %s, but got %s", newChange.getSort(), newChangeSet.getInternalId(), newChangeSet.getSourceFilename(), revision.getInternalId(), project.getName(), oldChange.getDescription(), newChange.getDescription());

                oldChange.setChecksum(newChange.getChecksum());
            }

            oldChangeSet.setChecksum(newChangeSet.getChecksum());
        }

        changeSetService.save(oldChangeSets);
    }

    /**
     * Performs indexing of database changes in the given project using the given local repository. The algorithm will
     * perform a breadth-first traversal from the root revision. For each revision during the traversal,
     * the whole local repository will checkout the given revision.
     * The traversal will abort when an already visited revision is met.
     *
     * @param project
     *         The project for which database changes shall be indexed.
     * @param localRepository
     *         The local repository connection.
     * @param revisionsToIndex
     *         The revisions that should be indexed.
     */
    private void indexDatabaseChanges(@NotNull Project project, @NotNull VcsLocalRepository localRepository, @NotNull Collection<Revision> revisionsToIndex) throws DatacTechnicalException {
        int indexed = 0;
        int newChangeSets = 0;

        for (Revision toIndex : revisionsToIndex) {
            double progress = (indexed++ / (double) revisionsToIndex.size()) * 100.0;
            projectService.saveAndPublishStateChange(project, progress);
            Collection<DatabaseChangeSet> changeSets = indexDatabaseChangesInRevision(project, localRepository, toIndex);
            if (changeSets != null) {
                newChangeSets += changeSets.size();
            }
        }

        eventLog.addMessage(new EventLogMessage(String.format("Indexed total of %s new change sets", newChangeSets)));
        LOGGER.info("Indexed total of {} new change sets in project {} [id={}]", newChangeSets, project.getName(), project.getId());
    }

    @NotNull
    private Set<Revision> orderRevisionsToIndex(@NotNull Project project, @NotNull Collection<Revision> revisionsToIndex) throws DatacTechnicalException {
        Revision rootRevision = revisionGraphService.findProjectRootRevision(project);
        rootRevision = revisionGraphService.findCachedByInternalIdAndProject(rootRevision.getInternalId(), project);
        Set<Revision> orderedRevisionsToIndex = new LinkedHashSet<Revision>();
        HashMap<String, AtomicInteger> mergeMap = new HashMap<>();
        breadthFirstTraverser.traverseChildrenCutOnMatch(rootRevision, r -> {
            if (revisionsToIndex.contains(r) && !orderedRevisionsToIndex.contains(r)) {
                orderedRevisionsToIndex.add(r);
            }
        }, (Revision r) -> {
            if (r.getChildren().size() == 1) {
                Revision child = r.getChildren().get(0);
                if (child.getParents().size() >= 2) {
                    // Whenever the next revision is a merge, don't continue on the current branch but wait until
                    // another branch reaches that revision
                    AtomicInteger pathsToVisit = mergeMap.computeIfAbsent(child.getInternalId(), x -> new AtomicInteger(child.getParents().size()));
                    return pathsToVisit.decrementAndGet() > 0;
                }
            }
            return false;
        });
        return orderedRevisionsToIndex;
    }

    private Collection<DatabaseChangeSet> indexDatabaseChangesInRevision(Project project, VcsLocalRepository localRepository, Revision revision) throws DatacTechnicalException {
        long byRevision = changeSetService.countByRevision(revision);
        if (byRevision == 0) {
            LOGGER.debug("Indexing database changes of project {} in revision {}", project.getName(), revision.getInternalId());
            List<DatabaseChangeSet> databaseChangeSets = listDatabaseChangeSetsInRevision(project, localRepository, revision);
            return changeSetService.linkRevisionsAndSave(databaseChangeSets, revision);
        } else {
            LOGGER.debug("Skipping database changes of project {} in revision {} because it was already indexed", project.getName(), revision.getInternalId());
        }
        return null;
    }

    @NotNull
    private List<DatabaseChangeSet> listDatabaseChangeSetsInRevision(Project project, VcsLocalRepository localRepository, Revision revision) throws DatacTechnicalException {
        // TODO: Check if the file even exists in that revision (somewhere -> maybe a bit earlier while calculating the list of revs to index)
        localRepository.checkoutRevision(revision);

        Optional<DatabaseChangeSystemAdapter> databaseChangeSystemAdapter = databaseChangeSystemAdapterRegistry.getAdapterByProject(project);
        List<DatabaseChangeSet> databaseChangeSets;
        if (databaseChangeSystemAdapter.isPresent()) {
            databaseChangeSets = databaseChangeSystemAdapter.get().listDatabaseChangeSetsForProject(project);
        } else {
            throw new MissingDatabaseChangeSystemAdapterException(project);
        }
        return databaseChangeSets;
    }
}
