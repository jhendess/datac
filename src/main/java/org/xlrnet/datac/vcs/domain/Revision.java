package org.xlrnet.datac.vcs.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;
import org.xlrnet.datac.foundation.domain.AbstractEntity;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.domain.validation.SameProjectParent;

/**
 * A revision represents a single versioning state in a VCS.
 */
@Entity
@Table(name = "revision")
@SameProjectParent
public class Revision extends AbstractEntity {

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
    @NotEmpty
    @Size(max = 256)
    @Column(name = "committer")
    private String committer;

    /**
     * Message that was published with the revision.
     */
    @Column(name = "message")
    private String message;

    /**
     * Timestamp when revision was originally created.
     */
    @Column(name = "commit_time")
    private LocalDateTime commitTime;

    /**
     * Project in which this revision exists.
     */
    @NotNull
    @OneToOne(optional = false)
    private Project project;

    /**
     * Parent revisions of this revision.
     */
    @ManyToMany(targetEntity = Revision.class, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "revision_graph",
            joinColumns = @JoinColumn(name = "revision_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "parent_revision_id", referencedColumnName = "id"))
    private List<Revision> parents = new ArrayList<>();

    public String getInternalId() {
        return internalId;
    }

    public Revision setInternalId(String internalId) {
        this.internalId = internalId;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public Revision setAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getCommitter() {
        return committer;
    }

    public Revision setCommitter(String committer) {
        this.committer = committer;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public Revision setMessage(String message) {
        this.message = message;
        return this;
    }

    public LocalDateTime getCommitTime() {
        return commitTime;
    }

    public Revision setCommitTime(LocalDateTime commitTime) {
        this.commitTime = commitTime;
        return this;
    }

    public List<Revision> getParents() {
        return parents;
    }

    public Revision setParents(List<Revision> parents) {
        this.parents = parents;
        return this;
    }

    public Revision addParent(Revision parent) {
        this.parents.add(parent);
        return this;
    }

    public Project getProject() {
        return project;
    }

    public Revision setProject(Project project) {
        this.project = project;
        return this;
    }
}
