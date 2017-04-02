package org.xlrnet.datac.vcs.api;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.vcs.domain.Branch;

/**
 * Local representation of a VCS repository. Local repositories are usually not thread-safe since they may modify the
 * local filesystem.
 */
public interface VcsLocalRepository {

    /**
     * Opens a connection to the remote repository of this local repository.
     *
     * @return a connection to the remote repository of this local repository.
     */
    @NotNull
    VcsRemoteRepositoryConnection connectToRemote() throws VcsConnectionException;

    /**
     * Updates the internal list of revisions from remote. Concrete implementations may decide if it is necessary to
     * perform a remote call to retrieve the latest revisions or not. If the fetching would e.g. take too long, it is
     * also sufficient to fetch the revisions directly during {@link #fetchLatestRevisionInBranch(Branch)} .
     *
     * @param branch
     *         The branch to fetch.
     */
    void updateRevisionsFromRemote(@NotNull Branch branch) throws DatacTechnicalException;

    /**
     * Returns the latest revision in the given branch. The returned revision contains a tree-structure for all parent
     * revisions. Depending on the concrete implementation, this method may invoke calls to a remote repository.
     *
     * @param branch
     *         The branch of which the revisions should be returned.
     * @return An {@link Iterable} containing the revisions of the given branch.
     */
    @NotNull
    VcsRevision fetchLatestRevisionInBranch(@NotNull Branch branch) throws DatacTechnicalException;
}
