package org.xlrnet.datac.vcs.impl;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.vcs.api.VcsAdapter;
import org.xlrnet.datac.vcs.api.VcsConnectionException;
import org.xlrnet.datac.vcs.api.VcsMetaInfo;
import org.xlrnet.datac.vcs.api.VcsRemoteRepository;

import java.net.URL;

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
    public VcsRemoteRepository connectRemote(@NotNull URL url, @NotNull String username, @NotNull String password) throws VcsConnectionException {
        return null;
    }
}
