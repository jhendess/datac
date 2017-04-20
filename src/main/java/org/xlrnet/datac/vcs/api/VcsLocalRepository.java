package org.xlrnet.datac.vcs.api;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
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

    /**
     * Returns a {@link Collection} of {@link VcsRevision} with all revisions where the given path was modified. This
     * method ignores the currently set branch and lists affected revisions in the whole repository. The returned
     * instances of {@link VcsRevision} don't need to have parents if the underlying implementation is lazy-loading,
     * since only the revisions affected by the change are important. The returned collection does not have to be in any
     * specific order. If the given path doesn't exist, an empty collection must be returned.
     *
     * @param path
     *         The path of the file or directory relative to the root of the repository which should be checked for
     *         modifying revisions.
     * @return An {@link Iterable} of all revisions where the given path was modified.
     * @throws VcsRepositoryException
     *         Will be thrown if the VCS repository encountered an internal error.
     */
    @NotNull
    Collection<VcsRevision> listRevisionsWithChangesInPath(@NotNull String path) throws VcsRepositoryException;

    /**
     * Performs a checkout operation for the local repository. This will reset all files in the local repository to the
     * exact state represented by the given revision. This method may modify the file system and is explicitly not
     * thread-safe. Note, that the given revision object is only guaranteed to have a valid internal id (i.e. {@link
     * VcsRevision#getInternalId()} returns a non-null value.
     *
     * @param revision
     *         The revision which should be checked out.
     */
    void checkoutRevision(@NotNull VcsRevision revision) throws VcsRepositoryException;

    /**
     * Performs a cleanup operation in the local repository if necessary. Depending on the concrete implementation, this
     * may e.g. reset lock files or remove dirty files which prevent regular operation. If no cleaning is necessary, the
     * method should return immediately.
     *
     * @throws VcsRepositoryException Will be thrown if the VCS repository encountered an internal error.
     */
    void cleanupIfNecessary() throws VcsRepositoryException;
}
