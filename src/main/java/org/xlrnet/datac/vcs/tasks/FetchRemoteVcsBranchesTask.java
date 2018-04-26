package org.xlrnet.datac.vcs.tasks;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.commons.tasks.AbstractRunnableTask;
import org.xlrnet.datac.vcs.api.VcsAdapter;
import org.xlrnet.datac.vcs.api.VcsRemoteCredentials;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;
import org.xlrnet.datac.vcs.domain.Branch;

import lombok.extern.slf4j.Slf4j;

/**
 * Task for fetching all available branches in a given remote VCS.
 */
@Slf4j
public class FetchRemoteVcsBranchesTask extends AbstractRunnableTask<Collection<Branch>> {

    /**
     * The VCS adapter that should be used for checking the connection.
     */
    private final VcsAdapter vcsAdapter;

    /**
     * Service for accessing vcs.
     */
    private final VcsRemoteCredentials credentials;

    public FetchRemoteVcsBranchesTask(@NotNull VcsAdapter vcsAdapter, @NotNull VcsRemoteCredentials credentials) {
        this.vcsAdapter = vcsAdapter;
        this.credentials = credentials;
    }

    @Override
    protected void runTask() {
        LOGGER.info("Begin fetching remote branches in VCS using adapter {}", vcsAdapter.getClass().getName());
        Collection<Branch> branches = null;
        try {
            VcsRemoteRepositoryConnection vcsRemoteRepository = vcsAdapter.connectRemote(credentials);
            branches = vcsRemoteRepository.listBranches();
            LOGGER.info("Found {} branches in {}", branches, credentials.getUrl());
            vcsRemoteRepository.close();
        } catch (Exception e) {
            LOGGER.error("Fetching remote branches failed", e);
        }
        getEntityChangeHandler().onChange(branches);
    }
}
