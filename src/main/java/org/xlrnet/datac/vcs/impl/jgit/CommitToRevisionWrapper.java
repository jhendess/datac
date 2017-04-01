package org.xlrnet.datac.vcs.impl.jgit;

import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xlrnet.datac.vcs.api.VcsRevision;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper which wraps {@link org.eclipse.jgit.revwalk.RevCommit} objects from JGit to {@link
 * org.xlrnet.datac.vcs.api.VcsRevision} objects.
 */
public class CommitToRevisionWrapper implements VcsRevision {

    private List<VcsRevision> parents;

    private final RevCommit wrapped;

    CommitToRevisionWrapper(RevCommit wrapped) {
        this.wrapped = wrapped;
    }

    @NotNull
    @Override
    public String getInternalId() {
        return wrapped.getId().getName();
    }

    @Override
    public String getMessage() {
        return wrapped.getFullMessage();
    }

    @Nullable
    @Override
    public String getAuthor() {
        return wrapped.getAuthorIdent().toExternalString();
    }

    @Nullable
    @Override
    public String getReviewer() {
        return wrapped.getCommitterIdent().toExternalString();
    }

    @Override
    public Instant getCommitTime() {
        return Instant.ofEpochSecond(wrapped.getCommitTime());
    }

    @NotNull
    @Override
    public synchronized List<? extends VcsRevision> getParents() {
        if (parents == null) {
            parents = new ArrayList<>(wrapped.getParentCount());
            for (RevCommit parent : wrapped.getParents()) {
                parents.add(new CommitToRevisionWrapper(parent));
            }
        }
        return parents;
    }
}
