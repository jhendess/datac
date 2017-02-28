package org.xlrnet.datac.vcs.api;

import org.jetbrains.annotations.NotNull;

/**
 * Connection object to a remote version control system. Implementations of this object may contain stateful data and
 * do not have to be thread-safe.
 */
public interface VcsRemoteRepository {

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
}
