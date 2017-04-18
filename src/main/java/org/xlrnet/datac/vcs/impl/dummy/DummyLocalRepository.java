package org.xlrnet.datac.vcs.impl.dummy;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.commons.exception.VcsRepositoryException;
import org.xlrnet.datac.vcs.api.VcsConnectionException;
import org.xlrnet.datac.vcs.api.VcsLocalRepository;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;
import org.xlrnet.datac.vcs.api.VcsRevision;
import org.xlrnet.datac.vcs.domain.Branch;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Dummy implementation of {@link VcsLocalRepository}.
 */
public class DummyLocalRepository implements VcsLocalRepository {

    @NotNull
    @Override
    public VcsRemoteRepositoryConnection connectToRemote() throws VcsConnectionException {
        return new DummyRemoteRepositoryConnection();
    }

    @Override
    public void updateRevisionsFromRemote(@NotNull Branch branch) throws VcsConnectionException {
        // TODO
    }

    @NotNull
    @Override
    public VcsRevision fetchLatestRevisionInBranch(@NotNull Branch branch) throws VcsConnectionException, VcsRepositoryException {
        return new DummyRevision().setInternalId("1");
    }

    @NotNull
    @Override
    public Collection<VcsRevision> listRevisionsWithChangesInPath(@NotNull String path) {
        return new ArrayList<>();
    }

    @Override
    public void checkoutRevision(@NotNull VcsRevision revision) throws VcsRepositoryException {
        // Do nothing
    }
}
