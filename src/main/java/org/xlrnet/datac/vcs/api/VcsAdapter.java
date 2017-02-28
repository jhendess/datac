package org.xlrnet.datac.vcs.api;

import org.jetbrains.annotations.NotNull;

import java.net.URL;

/**
 * Adapter interface used for communicating with a version control system.
 */
public interface VcsAdapter {

    /**
     * Returns an immutable {@link VcsMetaInfo} object containing meta data for the adaptor. This includes e.g. the name
     * of the adapter implementation, name of the VCS, etc.
     *
     * @return meta data for the adaptor.
     */
    @NotNull
    VcsMetaInfo getMetaInfo();

    /**
     * Connects to a remote VCS repository at the given target URL using the given authentication credentials. If the
     * connection did not fail, a {@link VcsRemoteRepository} object will be returned. Depending on the concrete
     * implementation, a {@link VcsRemoteRepository} object may also be returned without establishing an actual network
     * connection and validating the credentials.
     *
     * @param url
     *         The target repository to which should be connected.
     * @param username
     *         The username for authentication.
     * @param password
     *         The password for authentication.
     * @return A {@link VcsRemoteRepository} if no connection errors occurred.
     * @throws VcsConnectionException
     *         Will be thrown if an error occurred while connecting or authenticating.
     */
    @NotNull
    VcsRemoteRepository connectRemote(@NotNull URL url, @NotNull String username, @NotNull String password) throws VcsConnectionException;
}
