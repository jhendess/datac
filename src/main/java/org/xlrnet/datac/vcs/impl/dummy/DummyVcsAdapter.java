package org.xlrnet.datac.vcs.impl.dummy;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.vcs.api.*;

/**
 * Dummy implementation of a {@link VcsAdapter}.
 */
@Component
public class DummyVcsAdapter implements VcsAdapter {

    @NotNull
    @Override
    public VcsMetaInfo getMetaInfo() {
        return new DummyVcsMetaInfo();
    }

    @NotNull
    @Override
    public VcsRemoteRepositoryConnection connectRemote(@NotNull VcsRemoteCredentials credentials) throws VcsConnectionException {
        return new DummyRemoteRepositoryConnection();
    }
}
