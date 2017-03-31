package org.xlrnet.datac.vcs.services;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.domain.repository.RevisionRepository;

/**
 * Service for accessing and manipulating VCS revision graphs.
 */
@Service
public class RevisionGraphService extends AbstractTransactionalService<Revision, RevisionRepository> {

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
     * @param project The project.
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
     * @param project The project in which the revision should exist.
     * @param revisionId The id of the revision.
     * @return True if the revision exists, false if not.
     */
    @Transactional(readOnly = true)
    public boolean existsRevisionInProject(@NotNull Project project, @NotNull String revisionId) {
        return getRepository().countRevisionByInternalIdAndProject(revisionId, project) > 0;
    }

    /**
     * Fetches the revision with a given id in a given project.
     *
     * @param project The project in which the revision exists.
     * @param revisionId The revision id.
     * @return The revision if it exists, or null.
     */
    @Nullable
    @Transactional(readOnly = true)
    public Revision findRevisionInProject(@NotNull Project project, @NotNull String revisionId) {
        return getRepository().findByInternalIdAndProject(revisionId, project);
    }
}
