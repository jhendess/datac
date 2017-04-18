package org.xlrnet.datac.vcs.domain;

import org.hibernate.validator.constraints.NotEmpty;
import org.xlrnet.datac.commons.domain.TraversableNode;
import org.xlrnet.datac.foundation.domain.AbstractEntity;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.api.VcsRevision;
import org.xlrnet.datac.vcs.domain.validation.SameProjectParent;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A revision represents a single versioning state in a VCS.
 */
@Entity
@Table(name = "revision")
@SameProjectParent
public class Revision extends AbstractEntity implements VcsRevision, TraversableNode<Revision> {

    /**
     * Internal id used by the concrete VCS to identify a revision.
     */
    @NotEmpty
    @Size(max = 256)
    @Column(name = "internal_id")
    private String internalId;

    /**
     * Author who originally created the revision in the VCS.
     */
    @Size(max = 256)
    @Column(name = "author")
    private String author;

    /**
     * User who submitted the reviewed revision to the VCS.
     */
    @Size(max = 256)
    @Column(name = "reviewer")
    private String reviewer;

    /**
     * Message that was published with the revision.
     */
    @Column(name = "message")
    private String message;

    /**
     * Timestamp when revision was originally created.
     */
    @Column(name = "commit_time")
    private Instant commitTime;

    /**
     * Project in which this revision exists.
     */
    @NotNull
    @OneToOne(optional = false)
    private Project project;

    /**
     * Parent revisions of this revision. Persisting and merging must be executed manually in order to avoid stack
     * overflow errors.
     */
    @ManyToMany(targetEntity = Revision.class, cascade = {CascadeType.DETACH, CascadeType.REMOVE}, fetch = FetchType.EAGER)
    @JoinTable(name = "revision_graph",
            joinColumns = @JoinColumn(name = "revision_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "parent_revision_id", referencedColumnName = "id"))
    private List<Revision> parents = new ArrayList<>();

    /**
     * Direct children of this revision (inverse relationship).
     */
    @ManyToMany(mappedBy = "parents", fetch = FetchType.EAGER)
    private List<Revision> children = new ArrayList<>();


    public Revision() {
        // Empty constructor
    }

    /**
     * Copy-constructor from an external {@link VcsRevision} object. Parents won't be copied and remain empty.
     *
     * @param vcsRevision
     *         The object from which to create a revision entity.
     */
    public Revision(VcsRevision vcsRevision) {
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

    public Revision setInternalId(String internalId) {
        this.internalId = internalId;
        return this;
    }

    @Override
    public String getAuthor() {
        return author;
    }

    public Revision setAuthor(String author) {
        this.author = author;
        return this;
    }

    @Override
    public String getReviewer() {
        return reviewer;
    }

    public Revision setReviewer(String reviewer) {
        this.reviewer = reviewer;
        return this;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public Revision setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public Instant getCommitTime() {
        return commitTime;
    }

    public Revision setCommitTime(Instant commitTime) {
        this.commitTime = commitTime;
        return this;
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public List<Revision> getParents() {
        return parents;
    }

    public Revision setParents(List<Revision> parents) {
        this.parents = parents;
        return this;
    }

    public Revision addParent(@org.jetbrains.annotations.NotNull Revision parent) {
        if (!parents.contains(parent)) {
            this.parents.add(parent);
            parent.addChild(this);
        }
        return this;
    }

    public Revision addChild(@org.jetbrains.annotations.NotNull Revision child) {
        if (!children.contains(child)) {
            this.children.add(child);
            child.addParent(this);
        }
        return this;
    }

    public Revision replaceParent(@org.jetbrains.annotations.NotNull Revision oldParent, @org.jetbrains.annotations.NotNull Revision newParent) {
        checkArgument(parents.contains(oldParent), "Parents don't contain expected old parent");

        parents.remove(oldParent);
        addParent(newParent);

        return this;
    }

    public Project getProject() {
        return project;
    }

    public Revision setProject(Project project) {
        this.project = project;
        return this;
    }

    @org.jetbrains.annotations.NotNull
    @Override
    public List<Revision> getChildren() {
        return children;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Revision)) return false;
        Revision revision = (Revision) o;
        return Objects.equals(internalId, revision.internalId) &&
                Objects.equals(author, revision.author) &&
                Objects.equals(reviewer, revision.reviewer) &&
                Objects.equals(message, revision.message) &&
                Objects.equals(commitTime, revision.commitTime) &&
                Objects.equals(project, revision.project);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internalId, author, reviewer, message, commitTime, project);
    }
}
