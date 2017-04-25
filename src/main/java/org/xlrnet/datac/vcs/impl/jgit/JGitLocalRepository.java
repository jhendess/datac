package org.xlrnet.datac.vcs.impl.jgit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlrnet.datac.commons.exception.VcsRepositoryException;
import org.xlrnet.datac.foundation.services.FileService;
import org.xlrnet.datac.vcs.api.VcsConnectionException;
import org.xlrnet.datac.vcs.api.VcsLocalRepository;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;
import org.xlrnet.datac.vcs.api.VcsRevision;
import org.xlrnet.datac.vcs.domain.Branch;

/**
 * Implementation of a local git repository using JGit.
 */
public class JGitLocalRepository implements VcsLocalRepository {

    private static final String HEAD = "HEAD";

    private static Logger LOGGER = LoggerFactory.getLogger(JGitLocalRepository.class);

    /**
     * Local file path of the repository.
     */
    private final Path repositoryPath;

    /**
     * The target URL of the remote git repository.
     */
    private final String remoteRepositoryUrl;

    /**
     * The credentials provider for accessing the remote repository.
     */
    private final CredentialsProvider credentialsProvider;

    /**
     * Service for accessing the filesystem.
     */
    private final FileService fileService;

    JGitLocalRepository(Path repositoryPath, CredentialsProvider credentialsProvider, String remoteRepositoryUrl, FileService fileService) {
        this.repositoryPath = repositoryPath;
        this.credentialsProvider = credentialsProvider;
        this.remoteRepositoryUrl = remoteRepositoryUrl;
        this.fileService = fileService;
    }

    @NotNull
    @Override
    public VcsRemoteRepositoryConnection connectToRemote() throws VcsConnectionException {
        return new JGitRemoteRepositoryConnection(remoteRepositoryUrl, credentialsProvider);
    }

    @Override
    public synchronized void updateRevisionsFromRemote(@NotNull Branch branch) throws VcsConnectionException, VcsRepositoryException {
        String branchName = StringUtils.removeStartIgnoreCase(branch.getName(), "refs/heads/");
        LOGGER.debug("Fetching latest revisions from remote {} on branch {}", remoteRepositoryUrl, branchName);
        try (Git git = openRepository()) {
            if (!isBranchInRepository(git, branch.getName())) {
                git.checkout().setName(branchName).setForce(true).setCreateBranch(true).call();
            } else {
                git.checkout().setName(branchName).setForce(true).call();
            }
            git.pull().setCredentialsProvider(credentialsProvider).setRemoteBranchName(branchName).setStrategy(MergeStrategy.THEIRS).call();

            LOGGER.debug("Finished checking out from remote {} on branch {}", remoteRepositoryUrl, branchName);
        } catch (JGitInternalException | GitAPIException e) {
            LOGGER.error("Unexpected exception while communicating with git", e);
            throw new VcsConnectionException(e);
        } catch (IOException e) {
            LOGGER.error("Unexpected IOException", e);
            throw new VcsRepositoryException(e);
        }
    }

    private boolean isBranchInRepository(Git git, String branchName) throws GitAPIException {
        List<Ref> call = git.branchList().call();
        return call.stream().anyMatch(ref -> StringUtils.equals(branchName, ref.getName()));
    }

    @NotNull
    @Override
    public VcsRevision listLatestRevisionOnBranch(@NotNull Branch branch) throws VcsConnectionException, VcsRepositoryException {
        LOGGER.debug("Reading revisions on branch {} in repository {}", branch.getName(), repositoryPath.toString());
        try (Git git = openRepository()) {
            Repository repository = git.getRepository();
            Iterable<RevCommit> call = git.log()
                    .add(repository.resolve(branch.getName()))
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
        } catch (JGitInternalException | GitAPIException e) {
            LOGGER.error("Unexpected exception while communicating with git", e);
            throw new VcsConnectionException(e);
        } catch (IOException e) {
            LOGGER.error("Unexpected IOException", e);
            throw new VcsRepositoryException(e);
        }
    }

    @NotNull
    @Override
    public Collection<VcsRevision> listRevisionsWithChangesInPath(@NotNull String path) throws VcsRepositoryException {
        String cleanedPath = StringUtils.removeStart(path, "/").replace("\\", "/");
        LOGGER.debug("Listing affected revisions for path {} in repository {}", path, cleanedPath);
        try (Git git = openRepository()) {
            Iterable<RevCommit> call = git.log()
                    .all()
                    .addPath(cleanedPath)
                    .call();

            List<VcsRevision> affectedRevisions = new ArrayList<>();

            for (RevCommit revCommit : call) {
                affectedRevisions.add(new CommitToRevisionWrapper(revCommit));
                LOGGER.trace("ID: {}, Time: {}, Parents: {}, Message: {}", revCommit.getId(), revCommit.getCommitTime(), revCommit.getParentCount(), revCommit.getShortMessage().trim());
            }

            LOGGER.debug("Found {} affected revisions for path {} in repository {}", affectedRevisions.size(), path, cleanedPath);
            return affectedRevisions;
        } catch (JGitInternalException | GitAPIException e) {
            LOGGER.error("Unexpected exception while communicating with git", e);
            throw new VcsRepositoryException(e);
        } catch (IOException e) {
            LOGGER.error("Unexpected IOException", e);
            throw new VcsRepositoryException(e);
        }
    }

    @Override
    public void checkoutRevision(@NotNull VcsRevision revision) throws VcsRepositoryException {
        String internalId = revision.getInternalId();
        LOGGER.debug("Checking out revision {} in repository {}", internalId, repositoryPath);

        try (Git git = openRepository()) {
            git.checkout()
                    .setName(internalId)
                    .call();
            LOGGER.debug("Checkout done.");

        } catch (JGitInternalException | GitAPIException e) {
            LOGGER.error("Unexpected exception while communicating with git", e);
            throw new VcsRepositoryException(e);
        } catch (IOException e) {
            LOGGER.error("Unexpected IOException", e);
            throw new VcsRepositoryException(e);
        }
    }

    @Override
    public void cleanupIfNecessary() throws VcsRepositoryException {
        try (Git git = openRepository()) {
            if (isRepositoryLocked()) {
                unlockRepository();
            }

            boolean clean = git.status()
                    .call()
                    .isClean();

            if (!clean) {
                LOGGER.info("Repository {} is unclean - cleaning up", repositoryPath);
                git.clean().setCleanDirectories(true).call();
                git.reset().setRef(HEAD).setMode(ResetCommand.ResetType.HARD).call();
            }

        } catch (JGitInternalException | GitAPIException e) {
            LOGGER.error("Unexpected exception while communicating with git", e);
            throw new VcsRepositoryException(e);
        } catch (IOException e) {
            LOGGER.error("Unexpected IOException", e);
            throw new VcsRepositoryException(e);
        }
    }

    private void unlockRepository() throws IOException {
        LOGGER.info("Unlocking git repository {}", repositoryPath);
        fileService.deleteFile(getLockFilePath());
    }

    @NotNull
    private Git openRepository() throws IOException {
        return Git.open(repositoryPath.toFile());
    }

    private boolean isRepositoryLocked() {
        return Files.exists(getLockFilePath());
    }

    private Path getLockFilePath() {
        return repositoryPath.resolve(".git").resolve("index.lock");
    }
}
