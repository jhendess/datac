package org.xlrnet.datac.vcs.impl.dummy;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.exception.VcsRepositoryException;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.api.*;

import java.nio.file.Path;

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

    @NotNull
    @Override
    public VcsLocalRepository openLocalRepository(Project project, Path repositoryPath) throws VcsRepositoryException {
        return new DummyLocalRepository();
    }
}
