package org.xlrnet.datac.vcs.impl.dummy;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.vcs.api.VcsConnectionException;
import org.xlrnet.datac.vcs.api.VcsConnectionStatus;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;
import org.xlrnet.datac.vcs.domain.Branch;

import java.io.IOException;
import java.util.Collection;

/**
 * Dummy implementation of {@link VcsRemoteRepositoryConnection}.
 */
public class DummyRemoteRepositoryConnection implements VcsRemoteRepositoryConnection {

    private final Branch master = new Branch();

    private final Branch v1 = new Branch();

    private final Branch v2 = new Branch();

    DummyRemoteRepositoryConnection() {
        master.setName("master");
        master.setInternalId("1");
        v1.setName("1.0.x");
        v1.setInternalId("2");
        v2.setName("2.0.x");
        v2.setInternalId("3");
    }

    @NotNull
    @Override
    public VcsConnectionStatus checkConnection() throws VcsConnectionException {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            return VcsConnectionStatus.COMMUNICATION_FAILURE;
        }
        return VcsConnectionStatus.ESTABLISHED;
    }

    @Override
    @NotNull
    public Collection<Branch> listBranches() throws VcsConnectionException {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            return null;
        }
        return Lists.newArrayList(master, v1, v2);
    }

    @Override
    public void close() throws IOException {
        // Dummy needs no closing
    }
}
