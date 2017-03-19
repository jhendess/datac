package org.xlrnet.datac.vcs.domain;

import org.hibernate.validator.constraints.NotEmpty;
import org.xlrnet.datac.foundation.domain.AbstractEntity;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A revision represents a single versioning state in a VCS.
 */
@Entity
@Table(name = "revision")
public class Revision extends AbstractEntity {

    @NotEmpty
    @Size(max = 256)
    @Column(name = "internal_id")
    private String internalId;

    @Size(max = 256)
    @Column(name = "author")
    private String author;

    @NotEmpty
    @Size(max = 256)
    @Column(name = "committer")
    private String committer;

    @Column(name = "message")
    private String message;

    @Column(name = "commit_time")
    private LocalDateTime commitTime;

    @ManyToMany(targetEntity = Revision.class, cascade = CascadeType.ALL)
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
}
