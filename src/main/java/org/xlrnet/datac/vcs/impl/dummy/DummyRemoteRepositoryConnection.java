package org.xlrnet.datac.vcs.impl.dummy;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.vcs.api.VcsConnectionException;
import org.xlrnet.datac.vcs.api.VcsConnectionStatus;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;

import java.io.IOException;

/**
 * Dummy implementation of {@link VcsRemoteRepositoryConnection}.
 */
public class DummyRemoteRepositoryConnection implements VcsRemoteRepositoryConnection {

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
    public void close() throws IOException {
        // Dummy needs no closing
    }
}
