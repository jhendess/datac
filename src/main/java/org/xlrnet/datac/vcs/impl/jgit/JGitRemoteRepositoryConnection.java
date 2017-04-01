package org.xlrnet.datac.vcs.impl.jgit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.VcsRepositoryException;
import org.xlrnet.datac.vcs.api.VcsConnectionException;
import org.xlrnet.datac.vcs.api.VcsConnectionStatus;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;
import org.xlrnet.datac.vcs.domain.Branch;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Implementation of a remote git repository using JGit.
 */
public class JGitRemoteRepositoryConnection implements VcsRemoteRepositoryConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(JGitRemoteRepositoryConnection.class);

    private static final String NOT_AUTHORIZED = "not authorized";

    private static final String AUTHENTICATION_NOT_SUPPORTED = "authentication not supported";

    /** The target URL of the remote git repository. */
    private final String remoteRepositoryUrl;

    /** The credentials provider for accessing the remote repository. */
    private final CredentialsProvider provider;

    JGitRemoteRepositoryConnection(String remoteRepositoryUrl, CredentialsProvider provider) {
        this.remoteRepositoryUrl = remoteRepositoryUrl;
        this.provider = provider;
    }

    @NotNull
    @Override
    public VcsConnectionStatus checkConnection() throws VcsConnectionException {
        try {
            fetchRefs();
        } catch (GitAPIException e) {
            LOGGER.warn("Connection check failed", e);
            if (StringUtils.containsAny(e.getMessage(), NOT_AUTHORIZED, AUTHENTICATION_NOT_SUPPORTED)) {
                return VcsConnectionStatus.AUTHENTICATION_FAILURE;
            }
            return VcsConnectionStatus.INTERNAL_ERROR;
        }
        return VcsConnectionStatus.ESTABLISHED;
    }

    @NotNull
    @Override
    public Collection<Branch> listBranches() throws VcsConnectionException {
        try {
            LOGGER.debug("Fetching remote branches from {}", remoteRepositoryUrl);
            Collection<Ref> refCollection = fetchRefs();
            LOGGER.debug("Successfully fetched {} branches from {}", refCollection.size(), remoteRepositoryUrl);
            return refCollection.stream().map(this::refToBranch).collect(Collectors.toList());
        } catch (GitAPIException e) {
            LOGGER.error("Fetching remote branches from {} failed", remoteRepositoryUrl, provider);
            throw new VcsConnectionException(e);
        }
    }

    private Collection<Ref> fetchRefs() throws GitAPIException {
        return Git.lsRemoteRepository()
                .setHeads(true)
                .setTags(false)
                .setRemote(remoteRepositoryUrl)
                .setCredentialsProvider(provider)
                .call();
    }

    @Override
    public void initializeLocalRepository(@NotNull Path repositoryPath, @NotNull Branch branch) throws DatacTechnicalException, IOException, VcsConnectionException {
        String branchName = branch.getName();
        LOGGER.debug("Cloning repository from {} to {} on branch {}", remoteRepositoryUrl, repositoryPath, branchName);
        try (Git result = Git.cloneRepository()
                .setURI(remoteRepositoryUrl)
                .setDirectory(repositoryPath.toFile())
                .setBranch(branchName)
                .setCredentialsProvider(provider)
                .call()) {
            LOGGER.debug("Finished cloning repository to {}", result.getRepository().getDirectory().toPath().toString());
        } catch (TransportException | InvalidRemoteException te) {
            LOGGER.error("Cloning repository from {} to {} failed", te);
            throw new VcsConnectionException(te);
        } catch (GitAPIException e) {
            LOGGER.error("Cloning repository from {} to {} failed", e);
            throw new VcsRepositoryException(e);
        }
    }

    @Override
    public void close() throws IOException {
        // No connection closing necessary
    }

    private Branch refToBranch(Ref ref) {
        Branch branch = new Branch();
        branch.setName(ref.getName());
        branch.setInternalId(ref.getObjectId().getName());
        return branch;
    }
}
