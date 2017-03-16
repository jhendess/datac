package org.xlrnet.datac.vcs.tasks;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlrnet.datac.commons.tasks.AbstractRunnableTask;
import org.xlrnet.datac.vcs.api.VcsAdapter;
import org.xlrnet.datac.vcs.api.VcsConnectionStatus;
import org.xlrnet.datac.vcs.api.VcsRemoteCredentials;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;

/**
 * Task for checking if the connection to an external remote VCS can be established.
 */
public class CheckRemoteVcsConnectionTask extends AbstractRunnableTask<VcsConnectionStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckRemoteVcsConnectionTask.class);

    /** The VCS adapter that should be used for checking the connection. */
    private final VcsAdapter vcsAdapter;

    /** Service for accessing vcs. */
    private final VcsRemoteCredentials credentials;

    public CheckRemoteVcsConnectionTask(@NotNull VcsAdapter vcsAdapter, @NotNull VcsRemoteCredentials credentials) {
        this.vcsAdapter = vcsAdapter;
        this.credentials = credentials;
    }

    @Override
    protected void runTask() {
        LOGGER.info("Begin connection check to VCS using adapter {}", vcsAdapter.getClass().getName());
        VcsConnectionStatus vcsConnectionStatus = VcsConnectionStatus.INTERNAL_ERROR;
        try {
            VcsRemoteRepositoryConnection vcsRemoteRepository = vcsAdapter.connectRemote(credentials);
            vcsConnectionStatus = vcsRemoteRepository.checkConnection();
            LOGGER.info("Connection check returned status {}", vcsConnectionStatus);
            vcsRemoteRepository.close();
        } catch (Exception e) {
            LOGGER.error("Connection check failed", e);
        }
        getEntityChangeHandler().onChange(vcsConnectionStatus);
    }
}
