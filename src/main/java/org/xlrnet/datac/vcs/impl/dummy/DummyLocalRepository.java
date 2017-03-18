package org.xlrnet.datac.vcs.impl.dummy;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.commons.exception.VcsRepositoryException;
import org.xlrnet.datac.vcs.api.VcsConnectionException;
import org.xlrnet.datac.vcs.api.VcsLocalRepository;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;
import org.xlrnet.datac.vcs.api.VcsRevision;
import org.xlrnet.datac.vcs.domain.Branch;

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
    public void fetchLatestRevisions(@NotNull Branch branch) throws VcsConnectionException {
        // TODO
    }

    @NotNull
    @Override
    public Iterable<VcsRevision> listRevisionsInBranch(@NotNull Branch branch) throws VcsConnectionException, VcsRepositoryException {
        // TODO

        return ArrayIterator::new;
    }
}