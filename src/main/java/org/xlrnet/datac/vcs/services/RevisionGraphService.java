package org.xlrnet.datac.vcs.services;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.commons.domain.LimitOffsetPageable;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.vcs.api.VcsRevision;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.domain.repository.RevisionRepository;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

/**
 * Service for accessing and manipulating VCS revision graphs.
 */
@Service
public class RevisionGraphService extends AbstractTransactionalService<Revision, RevisionRepository> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RevisionGraphService.class);

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *
     * @param crudRepository
     *         The crud repository for providing basic crud operations.
     */
    @Autowired
    public RevisionGraphService(RevisionRepository crudRepository) {
        super(crudRepository);
    }

    /**
     * Find all revisions of a single project.
     *
     * @param project
     *         The project.
     * @return All unordered revisions in the project.
     */
    @NotNull
    @Transactional(readOnly = true)
    public List<Revision> findAllByProject(@NotNull Project project) {
        return getRepository().findAllByProject(project);
    }

    /**
     * Checks if a revision with the given ID exists in the given project.
     *
     * @param project
     *         The project in which the revision should exist.
     * @param revisionId
     *         The id of the revision.
     * @return True if the revision exists, false if not.
     */
    @Transactional(readOnly = true)
    public boolean existsRevisionInProject(@NotNull Project project, @NotNull String revisionId) {
        return getRepository().countRevisionByInternalIdAndProject(revisionId, project) > 0;
    }

    public Revision findByInternalIdAndProject(String internalId, Project project) {
        return getRepository().findByInternalIdAndProject(internalId, project);
    }

    /**
     * Returns a list of the last revisions in the given project. The returned revisions are ordered by their timestamp.
     * Note, that this orders only by the timestamp when the revision was committed.
     *
     * @param project
     *         The project in which the revisions must lie.
     * @param limit
     *         Amount of revisions to find.
     * @return a list of the last revisions in the given project.
     */
    public List<Revision> findLastRevisionsInProject(Project project, int limit) {
        return getRepository().findAllByProject(project, new LimitOffsetPageable(limit, 0, new Sort(
                new Sort.Order(Sort.Direction.DESC, "commitTime"))
        ));
    }

    /**
     * Fetches the revision with a given id in a given project.
     *
     * @param project
     *         The project in which the revision exists.
     * @param revisionId
     *         The revision id.
     * @return The revision if it exists, or null.
     */
    @Nullable
    @Transactional(readOnly = true)
    public Revision findRevisionInProject(@NotNull Project project, @NotNull String revisionId) {
        return getRepository().findByInternalIdAndProject(revisionId, project);
    }

    /**
     * Performs a safe save operation without causing a stack overflow by performing a depth-first traversal and saving
     * each revision on its own.
     *
     * @param revision
     *         The revision to save.
     * @return The saved revision.
     */
    @Override
    @Transactional
    public Revision save(@NotNull Revision revision) {
        LOGGER.trace("Begin safe saving of revision graph");
        Multimap<Revision, Revision> revisionChildMap = MultimapBuilder.hashKeys().linkedListValues().build();
        Deque<Revision> revisionStack = new LinkedList<>();
        revisionStack.push(revision);

        while (!revisionStack.isEmpty()) {
            Revision nextRevision = revisionStack.peek();
            if (nextRevision.getParents().isEmpty()) {
                saveAndReplaceChildren(nextRevision, revisionChildMap);
                revisionStack.pop();
                continue;
            }
            boolean allParentsSave = true;
            for (Revision parent : nextRevision.getParents()) {
                revisionChildMap.put(parent, nextRevision);
                if (!parent.isPersisted()) {
                    revisionStack.push(parent);
                }
                allParentsSave &= parent.isPersisted();
            }
            // Replace children after all parents were saved
            if (allParentsSave) {
                saveAndReplaceChildren(nextRevision, revisionChildMap);
                revisionStack.pop();
            }
        }
        LOGGER.trace("Finished safe saving of revision graph");

        return findRevisionInProject(revision.getProject(), revision.getInternalId());
    }


    /**
     * Returns the root revision in a project - i.e. the revision without any parents.
     *
     * @param project
     *         The project to check
     * @return
     */
    @Transactional(readOnly = true)
    public Revision findProjectRootRevision(Project project) {
        checkArgument(project.isPersisted(), "Project must be persisted");
        return getRepository().findProjectRootRevision(project.getId());
    }

    /**
     * Finds all internal {@link Revision} objects which have the same internal id as the {@link VcsRevision} objects in
     * the given collection. If an object does not exist, it will throw a null pointer exception.
     *
     * @param project
     *         The project in which the revisions must exist
     * @param externalRevisions
     *         The external revisions from a VCS that should be found.
     * @return Internal revision objects with the same internal ids as the given external revision objects.
     */
    @Transactional(readOnly = true)
    public Collection<Revision> findMatchingInternalRevisions(Project project, Collection<VcsRevision> externalRevisions) {
        Set<Revision> internalRevisions = new HashSet<>();
        for (VcsRevision externalRevision : externalRevisions) {
            Revision internalRevision = getRepository().findByInternalIdAndProject(externalRevision.getInternalId(), project);
            LOGGER.debug("Revision {} was not found in project {} [id={}]", externalRevision.getInternalId(), project.getName(), project.getId());
            if (internalRevision != null) {
                internalRevisions.add(internalRevision);
            }
        }

        return internalRevisions;
    }

    private void saveAndReplaceChildren(Revision revisionToPersist, Multimap<Revision, Revision> revisionChildMap) {
        LOGGER.trace("Saving revision {}", revisionToPersist.getInternalId());
        Revision persistedRevision = super.save(revisionToPersist);
        for (Revision child : revisionChildMap.get(revisionToPersist)) {
            child.replaceParent(revisionToPersist, persistedRevision);
        }
    }
}
