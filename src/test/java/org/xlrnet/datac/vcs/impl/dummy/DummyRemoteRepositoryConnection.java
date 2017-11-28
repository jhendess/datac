package org.xlrnet.datac.vcs.impl.dummy;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.vcs.api.VcsConnectionException;
import org.xlrnet.datac.vcs.api.VcsConnectionStatus;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;
import org.xlrnet.datac.vcs.domain.Branch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;

/**
 * Dummy implementation of {@link VcsRemoteRepositoryConnection}.
 */
public class DummyRemoteRepositoryConnection implements VcsRemoteRepositoryConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummyRemoteRepositoryConnection.class);

    private final Branch master = new Branch();

    private final Branch v1 = new Branch();

    DummyRemoteRepositoryConnection() {
        master.setName("master");
        master.setInternalId("1");
        v1.setName("1.0.x");
        v1.setInternalId("2");
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
        return Lists.newArrayList(master, v1);
    }

    @Override
    public void initializeLocalRepository(@NotNull Path repositoryPath, @NotNull Branch branch) throws DatacTechnicalException, IOException {
        LOGGER.debug("Initializing local repository for branch {} in {}", branch, repositoryPath);

        Path dummyFile = repositoryPath.resolve("dummy.txt");
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(dummyFile)) {
            bufferedWriter.append(Instant.now().toString());
            bufferedWriter.append("\nPROJECT: ").append(branch.getProject().getName());
            bufferedWriter.append("\nBRANCH: ").append(branch.getInternalId());
            bufferedWriter.flush();
        }
    }

    @Override
    public void close() throws IOException {
        // Dummy needs no closing
    }
}
