package org.xlrnet.datac.vcs.api;

import org.jetbrains.annotations.NotNull;

/**
 * Adapter interface used for communicating with a version control system. Adapters must be implemented stateless as
 * they are used for instantiating new connections and will be shared among multiple threads.
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
     * Connects to a remote VCS repository at the given target URL using the given authentication credentials. For each
     * new connection, a new connection object should be returned.
     * If the connection did not fail, a {@link VcsRemoteRepositoryConnection} object will be returned.
     * Depending on the concrete implementation, a {@link VcsRemoteRepositoryConnection} object may also be returned
     * without establishing an actual network connection and validating the credentials.
     *
     * @param credentials
     *         Credentials for the remote repository.
     * @throws VcsConnectionException
     *         Will be thrown if an error occurred while connecting or authenticating.
     */
    @NotNull
    VcsRemoteRepositoryConnection connectRemote(@NotNull VcsRemoteCredentials credentials) throws VcsConnectionException;
}
