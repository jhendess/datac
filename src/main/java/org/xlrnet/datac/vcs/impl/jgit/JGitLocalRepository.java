package org.xlrnet.datac.vcs.impl.jgit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
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
import java.util.Iterator;

/**
 * Implementation of a local git repository using JGit.
 */
public class JGitLocalRepository implements VcsLocalRepository {

    private static Logger LOGGER = LoggerFactory.getLogger(JGitLocalRepository.class);

    /** The interval in which fetches may be performed. */
    private static final int MAXIMUM_FETCH_INTERVAL = 30000;

    /** Local file path of the repository. */
    private final Path repositoryPath;

    /** The target URL of the remote git repository. */
    private final String remoteRepositoryUrl;

    /** The credentials provider for accessing the remote repository. */
    private final CredentialsProvider credentialsProvider;

    /** Timestamp when the last remote fetch was performed. */
    private long lastFetch;

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
        if (System.currentTimeMillis() - lastFetch < MAXIMUM_FETCH_INTERVAL) {
            LOGGER.debug("Skipping fetch request");
            return;
        }
        lastFetch = System.currentTimeMillis();
        String branchName = branch.getName();
        LOGGER.debug("Fetching latest revisions from remote {} on branch {}", remoteRepositoryUrl, branchName);
        try (Git git = openRepository()) {
            FetchResult result = git.fetch()
                    .setCredentialsProvider(credentialsProvider)
                    .call();
            if (StringUtils.isNotBlank(result.getMessages())) {
                LOGGER.debug("Fetch from remote {} returned messages: {}", result.getMessages());
                //throw new VcsRepositoryException("Pull from remote " + remoteRepositoryUrl + " on branch " + branchName + " failed");
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
        try (Git git = openRepository()) {
            Repository repository = git.getRepository();
            Iterable<RevCommit> call = git.log()
                    .add(repository.resolve(branch.getInternalId()))
                    .call();

            Iterator<RevCommit> iterator = call.iterator();
            RevCommit next = iterator.next();
            CommitToRevisionWrapper commitToRevisionWrapper = new CommitToRevisionWrapper(next);

            do {
                // Just walk over all commits to initialize them correctly...
                LOGGER.trace("ID: {}, Time: {}, Parents: {}, Message: {}", next.getId(), next.getCommitTime(), next.getParentCount(), next.getShortMessage().trim());
                next = iterator.hasNext() ? iterator.next() : null;
            } while (next != null);

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

    @NotNull
    private Git openRepository() throws IOException {
        return Git.open(repositoryPath.toFile());
    }
}
