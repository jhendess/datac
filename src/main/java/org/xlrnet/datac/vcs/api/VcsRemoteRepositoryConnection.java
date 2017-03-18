package org.xlrnet.datac.vcs.api;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.vcs.domain.Branch;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Connection object to a remote version control system. Implementations of this object may contain stateful data and
 * do not have to be thread-safe.
 */
public interface VcsRemoteRepositoryConnection extends Closeable {

    /**
     * Performs a request to the VCS backend and tries to login with the instance's credentials. The status of the
     * connection will be returned as a {@link VcsConnectionStatus}. Exceptions may only be thrown as wrappers for
     * technical exception and not if the connection failed e.g. because of invalid credentials.
     *
     * @return The connection status of the connection.
     * @throws VcsConnectionException
     *         Will only be thrown if an actual technical exception occurred while trying to connect.
     */
    @NotNull
    VcsConnectionStatus checkConnection() throws VcsConnectionException;

    /**
     * Lists all branches in a remote VCS.
     *
     * @return list of all branches in a VCS
     * @throws VcsConnectionException
     *         Will be thrown if any connection errors occurred.
     */
    @NotNull
    Collection<Branch> listBranches() throws VcsConnectionException;

    /**
     * Initialize a project repository on the local filesystem. The repository must afterwards be readable by the {@link
     * VcsLocalRepository} counterpart for this adapter.
     *
     * @param repositoryPath
     *         The target path which should be used as the root directory for the local repository.
     * @param branch
     *         The branch that should be used for initializing the local repository.
     * @throws DatacTechnicalException
     *         May be thrown in case of an error.
     * @throws IOException
     *         May be thrown if an error occurred while writing to the local file system.
     */
    void initializeLocalRepository(@NotNull Path repositoryPath, @NotNull Branch branch) throws DatacTechnicalException, IOException;
}
