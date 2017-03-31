package org.xlrnet.datac.vcs.impl.dummy;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.vcs.api.VcsRevision;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dummy implementation of {@link VcsRevision}.
 */
public class DummyRevision implements VcsRevision {

    /**
     * Internal id used by the concrete VCS to identify a DummyRevision.
     */
    private String internalId;

    /**
     * Author who originally created the DummyRevision in the VCS.
     */
    private String author;

    /**
     * User who submitted the reviewed DummyRevision to the VCS.
     */
    private String reviewer;

    /**
     * Message that was published with the DummyRevision.
     */
    private String message;

    /**
     * Timestamp when DummyRevision was originally created.
     */
    private LocalDateTime commitTime;

    private List<DummyRevision> parents = new ArrayList<>();


    public DummyRevision() {
        // Empty constructor
    }

    /**
     * Copy-constructor from an external {@link VcsRevision} object. Parents won't be copied and remain empty.
     *
     * @param vcsRevision
     *         The object from which to create a DummyRevision entity.
     */
    public DummyRevision(VcsRevision vcsRevision) {
        setInternalId(vcsRevision.getInternalId());
        setMessage(vcsRevision.getMessage());
        setAuthor(vcsRevision.getAuthor());
        setReviewer(vcsRevision.getReviewer());
        setCommitTime(vcsRevision.getCommitTime());
    }

    @Override
    @org.jetbrains.annotations.NotNull
    public String getInternalId() {
        return internalId;
    }

    public DummyRevision setInternalId(String internalId) {
        this.internalId = internalId;
        return this;
    }

    @Override
    public String getAuthor() {
        return author;
    }

    public DummyRevision setAuthor(String author) {
        this.author = author;
        return this;
    }

    @Override
    public String getReviewer() {
        return reviewer;
    }

    public DummyRevision setReviewer(String reviewer) {
        this.reviewer = reviewer;
        return this;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public DummyRevision setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public LocalDateTime getCommitTime() {
        return commitTime;
    }

    public DummyRevision setCommitTime(LocalDateTime commitTime) {
        this.commitTime = commitTime;
        return this;
    }

    @NotNull
    @Override
    public List<DummyRevision> getParents() {
        return parents;
    }

    public DummyRevision setParents(List<DummyRevision> parents) {
        this.parents = parents;
        return this;
    }

    public DummyRevision addParent(DummyRevision parent) {
        this.parents.add(parent);
        return this;
    }
}
