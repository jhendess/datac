package org.xlrnet.datac.vcs.impl.jgit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.commons.exception.VcsRepositoryException;
import org.xlrnet.datac.vcs.api.VcsConnectionException;
import org.xlrnet.datac.vcs.api.VcsLocalRepository;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;
import org.xlrnet.datac.vcs.api.VcsRevision;
import org.xlrnet.datac.vcs.domain.Branch;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Implementation of a local git repository using JGit.
 */
public class JGitLocalRepository implements VcsLocalRepository {

    private static final int MAXIMUM_FETCH_INTERVAL = 10000;

    private static Logger LOGGER = LoggerFactory.getLogger(JGitLocalRepository.class);

    /** Local file path of the repository. */
    private final Path repositoryPath;

    /** The target URL of the remote git repository. */
    private final String remoteRepositoryUrl;

    /** The credentials provider for accessing the remote repository. */
    private final CredentialsProvider credentialsProvider;

    JGitLocalRepository(Path repositoryPath, CredentialsProvider credentialsProvider, String remoteRepositoryUrl) {
        this.repositoryPath = repositoryPath;
        this.credentialsProvider = credentialsProvider;
        this.remoteRepositoryUrl = remoteRepositoryUrl;
    }

    @NotNull
    @Override
    public VcsRemoteRepositoryConnection connectToRemote() throws VcsConnectionException {
        return new JGitRemoteRepositoryConnection(remoteRepositoryUrl, credentialsProvider);
    }

    @Override
    public synchronized void updateRevisionsFromRemote(@NotNull Branch branch) throws VcsConnectionException, VcsRepositoryException {
        String branchName = branch.getName();
        LOGGER.debug("Pulling latest revisions from remote {} on branch {}", remoteRepositoryUrl, branchName);
        try (Git git = Git.open(repositoryPath.toFile())) {
            PullResult result = git.pull()
                    .setRemoteBranchName(branchName)
                    .setCredentialsProvider(credentialsProvider)
                    .call();
            if (!result.isSuccessful()) {
                LOGGER.error("Pull from remote {} on branch {} failed", remoteRepositoryUrl, branchName);
                throw new VcsRepositoryException("Pull from remote " + remoteRepositoryUrl + " on branch " + branchName + " failed");
            }
            LOGGER.debug("Finished fetching latest revisions from remote {} on branch {}", remoteRepositoryUrl, branchName);
        } catch (GitAPIException e) {
            LOGGER.error("Unexpected exception while communicating with git", e);
            throw new VcsConnectionException(e);
        } catch (IOException e) {
            LOGGER.error("Unexpected IOException", e);
            throw new VcsRepositoryException(e);
        }
    }

    @NotNull
    @Override
    public VcsRevision fetchLatestRevisionInBranch(@NotNull Branch branch) throws VcsConnectionException, VcsRepositoryException {
        LOGGER.debug("Reading revisions on branch {} in repository {}", branch.getName(), repositoryPath.toString());
        try (Git git = Git.open(repositoryPath.toFile())) {
            Repository repository = git.getRepository();
            Iterable<RevCommit> call = git.log()
                    .add(repository.resolve(branch.getInternalId()))
                    .call();

            CommitToRevisionWrapper commitToRevisionWrapper = new CommitToRevisionWrapper(call.iterator().next());

            for (RevCommit revCommit : call) {
                // Just walk over all commits to initialize them correctly...
                LOGGER.trace("ID: {}, Time: {}, Parents: {}, Message: {}", revCommit.getId(), revCommit.getCommitTime(), revCommit.getParentCount(), revCommit.getFullMessage().trim());
            }

            LOGGER.debug("Finished reading revisions on branch {} in repository {}", branch.getName(), repositoryPath.toString());
            return commitToRevisionWrapper;
        } catch (GitAPIException e) {
            LOGGER.error("Unexpected exception while communicating with git", e);
            throw new VcsConnectionException(e);
        } catch (IOException e) {
            LOGGER.error("Unexpected IOException", e);
            throw new DatacRuntimeException(e);
        }
    }
}
