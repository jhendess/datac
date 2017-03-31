package org.xlrnet.datac.vcs.api;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.commons.exception.VcsRepositoryException;
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
     * perform a remote call to retrieve the latest revisions or not. If the fetching would e.g. take too long, it also
     * sufficient to fetch the revisions directly during {@link #fetchLatestRevisionInBranch(Branch)} .
     *
     * @param branch
     *         The branch to fetch.
     */
    void fetchLatestRevisions(@NotNull Branch branch) throws VcsConnectionException;

    /**
     * Creates an {@link Iterable} over all revisions in the given branch. The first element of the iterable is always
     * the latest revision, while each next revision gets older. The latest element in the iterable is therefore the
     * root revision of the branch.
     * Depending on the concrete implementation, this method may invoke calls to a remote repository.
     *
     * @param branch
     *         The branch of which the revisions should be returned.
     * @return An {@link Iterable} containing the revisions of the given branch.
     */
    @NotNull
    VcsRevision fetchLatestRevisionInBranch(@NotNull Branch branch) throws VcsConnectionException, VcsRepositoryException;
}
