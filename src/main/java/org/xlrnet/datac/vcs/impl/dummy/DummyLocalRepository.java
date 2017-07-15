package org.xlrnet.datac.vcs.impl.dummy;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlrnet.datac.commons.exception.VcsRepositoryException;
import org.xlrnet.datac.database.impl.dummy.DummyDcsAdapter;
import org.xlrnet.datac.vcs.api.VcsConnectionException;
import org.xlrnet.datac.vcs.api.VcsLocalRepository;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;
import org.xlrnet.datac.vcs.api.VcsRevision;
import org.xlrnet.datac.vcs.domain.Branch;

import java.time.Instant;
import java.util.Collection;

/**
 * Dummy implementation of {@link VcsLocalRepository}.
 */
public class DummyLocalRepository implements VcsLocalRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummyLocalRepository.class);

    @NotNull
    @Override
    public VcsRemoteRepositoryConnection connectToRemote() throws VcsConnectionException {
        return new DummyRemoteRepositoryConnection();
    }

    @Override
    public void updateRevisionsFromRemote(@NotNull Branch branch) throws VcsConnectionException {
        // Do nothing
    }

    @NotNull
    @Override
    public VcsRevision listLatestRevisionOnBranch(@NotNull Branch branch) throws VcsConnectionException, VcsRepositoryException {
        String headId = "master".equalsIgnoreCase(branch.getName()) ? "1" : "0";

        DummyRevision root = new DummyRevision("6").setCommitTime(Instant.now());

        return new DummyRevision(headId).setCommitTime(Instant.now())
                .addParent(
                        new DummyRevision("2").setCommitTime(Instant.now())
                                .addParent(
                                        new DummyRevision("3").setCommitTime(Instant.now())
                                                .addParent(
                                                        new DummyRevision("5").setCommitTime(Instant.now())
                                                                .addParent(root)
                                                )
                                )
                                .addParent(
                                        new DummyRevision("4").setCommitTime(Instant.now())
                                                .addParent(root)
                                )
                );
    }

    @NotNull
    @Override
    public Collection<VcsRevision> listRevisionsWithChangesInPath(@NotNull String path) {
        return ImmutableList.of(
                new DummyRevision("6").setCommitTime(Instant.now()),
                new DummyRevision("5").setCommitTime(Instant.now()),
                new DummyRevision("4").setCommitTime(Instant.now()),
                new DummyRevision("3").setCommitTime(Instant.now()),
                new DummyRevision("2").setCommitTime(Instant.now()),
                new DummyRevision("1").setCommitTime(Instant.now())
        );
    }

    @Override
    public void checkoutRevision(@NotNull VcsRevision revision) throws VcsRepositoryException {
        int revisionNumber = NumberUtils.toInt(revision.getInternalId());
        LOGGER.debug("Changing state in DCS adaptor to revision {}", revisionNumber);
        DummyDcsAdapter.CHECKED_OUT_REVISION = revisionNumber;
    }

    @Override
    public void cleanupIfNecessary() throws VcsRepositoryException {
        // Do nothing
    }
}
