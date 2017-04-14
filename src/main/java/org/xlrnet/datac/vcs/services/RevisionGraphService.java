package org.xlrnet.datac.vcs.services;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.domain.repository.RevisionRepository;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

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

    private void saveAndReplaceChildren(Revision revisionToPersist, Multimap<Revision, Revision> revisionChildMap) {
        LOGGER.trace("Saving revision {}", revisionToPersist.getInternalId());
        Revision persistedRevision = super.save(revisionToPersist);
        for (Revision child : revisionChildMap.get(revisionToPersist)) {
            child.replaceParent(revisionToPersist, persistedRevision);
        }
    }
}
