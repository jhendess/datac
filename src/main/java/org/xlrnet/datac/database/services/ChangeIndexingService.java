package org.xlrnet.datac.database.services;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.MissingDatabaseChangeSystemAdapter;
import org.xlrnet.datac.commons.graph.BreadthFirstTraverser;
import org.xlrnet.datac.commons.graph.DepthFirstTraverser;
import org.xlrnet.datac.database.api.DatabaseChangeSystemAdapter;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for indexing database changes in a project.
 */
@Service
public class ChangeIndexingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeIndexingService.class);

    /** Transaction timeout for indexing is increased to avoid errors. */
    private static final int INDEX_TRANSACTION_TIMEOUT = 600;

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


    @Transactional(timeout = INDEX_TRANSACTION_TIMEOUT)
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
        LOGGER.warn("Parent directory of change log may not be null - this is probably a bug in the VCS adapter");
        if (parentPath != null) {
            changeLogRevisions = localRepository.listRevisionsWithChangesInPath(parentPath.toString());
        }

        project.setState(ProjectState.INDEXING);
        Project updatedProject = projectService.save(project);
        projectService.saveAndPublishStateChange(updatedProject, 0);

        // Convert the external revisions to internal ones
        Collection<Revision> internalRevisions = revisionGraphService.findMatchingInternalRevisions(updatedProject, changeLogRevisions);
        // Find those revisions which don't have a change set yet
        List<Revision> revisionsToIndex = new ArrayList<>();
        for (Revision revision : internalRevisions) {
            if (changeSetService.countByRevision(revision) == 0) {
                revisionsToIndex.add(revision);
            }
        }

        if (!revisionsToIndex.isEmpty()) {
            LOGGER.debug("Indexing {} revisions", revisionsToIndex.size());
            indexDatabaseChanges(updatedProject, localRepository, revisionsToIndex);
        } else {
            LOGGER.info("No new revisions in project {} [id={}]", updatedProject.getName(), updatedProject.getId());
        }

        return updatedProject;
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
        Revision rootRevision = revisionGraphService.findProjectRootRevision(project);
        AtomicInteger indexed = new AtomicInteger(0);
        AtomicInteger newChangeSets = new AtomicInteger(0);

        // FIXME: breadth-first traversing doesn't honor the correct order in which revision were originally committed - try depth-first instead
        breadthFirstTraverser.traverseChildren(rootRevision, (Revision r) -> {
            if (revisionsToIndex.contains(r)) {
                double progress = (indexed.getAndIncrement() / (double) revisionsToIndex.size()) * 100.0;
                projectService.saveAndPublishStateChange(project, progress);
                Collection<DatabaseChangeSet> changeSets = indexDatabaseChangesInRevision(project, localRepository, r);
                newChangeSets.addAndGet(changeSets.size());
            }
        });
        eventLog.addMessage(new EventLogMessage(String.format("Indexed total of %s new change sets", newChangeSets.get())));
        LOGGER.info("Indexed total of {} new change sets in project {} [id={}]", newChangeSets.get(), project.getName(), project.getId());
    }

    private Collection<DatabaseChangeSet> indexDatabaseChangesInRevision(Project project, VcsLocalRepository localRepository, Revision revision) throws DatacTechnicalException {
        LOGGER.debug("Indexing database changes of project {} in revision {}", project.getName(), revision.getInternalId());

        localRepository.checkoutRevision(revision);

        Optional<DatabaseChangeSystemAdapter> databaseChangeSystemAdapter = databaseChangeSystemAdapterRegistry.getAdapterByProject(project);
        List<DatabaseChangeSet> databaseChangeSets;
        if (databaseChangeSystemAdapter.isPresent()) {
            databaseChangeSets = databaseChangeSystemAdapter.get().listDatabaseChangeSetsForProject(project);
        } else {
            throw new MissingDatabaseChangeSystemAdapter(project);
        }

        LOGGER.trace("Linking change sets to revisions");

        for (DatabaseChangeSet databaseChangeSet : databaseChangeSets) {
            databaseChangeSet.setRevision(revision);
            changeSetService.linkRevisions(databaseChangeSet);
        }

        return changeSetService.save(databaseChangeSets);
    }
}
