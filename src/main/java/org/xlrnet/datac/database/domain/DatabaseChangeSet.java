package org.xlrnet.datac.database.domain;

import org.hibernate.validator.constraints.NotEmpty;
import org.xlrnet.datac.foundation.domain.AbstractEntity;
import org.xlrnet.datac.foundation.domain.Sortable;
import org.xlrnet.datac.foundation.domain.validation.Sorted;
import org.xlrnet.datac.vcs.domain.Revision;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * Set of database changes that must be executed together.
 */
@Entity
@Table(name = "changeset")
public class DatabaseChangeSet extends AbstractEntity implements Sortable {

    /**
     * Internal id of the change set.
     */
    @NotNull
    @Size(max = 256)
    @Column(name = "internal_id")
    private String internalId;

    /**
     * Comment which describes the change set.
     */
    @Column(name = "comment")
    private String comment;

    /**
     * Author of the change set (any string).
     */
    @NotEmpty
    @Size(max = 128)
    @Column(name = "author")
    private String author;

    /**
     * Checksum for this change set.
     */
    @NotEmpty
    @Size(max = 256)
    @Column(name = "checksum")
    private String checksum;

    /**
     * Integer to sort by. Defines in which order the change sets must be executed.
     */
    @Min(0)
    @Column(name = "sort")
    private int sort;       // TODO: Validate unique sorting

    /**
     * The revision to which this change set belongs.
     */
    @JoinColumn(name = "revision_id")
    @OneToOne(targetEntity = Revision.class, cascade = {CascadeType.REMOVE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private Revision revision;

    /**
     * The changes in this change set.
     */
    @Valid
    @Sorted
    @OneToMany(targetEntity = DatabaseChange.class, cascade = CascadeType.ALL)
    @JoinColumn(name = "changeset_id", referencedColumnName = "id", nullable = false)
    private List<DatabaseChange> changes = new ArrayList<>();

    public DatabaseChangeSet addChange(DatabaseChange datacChange) {
        if (!changes.contains(datacChange)) {
            changes.add(datacChange);
        }
        return this;
    }

    public String getInternalId() {
        return internalId;
    }

    public DatabaseChangeSet setInternalId(String internalId) {
        this.internalId = internalId;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public DatabaseChangeSet setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public DatabaseChangeSet setAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getChecksum() {
        return checksum;
    }

    public DatabaseChangeSet setChecksum(String checksum) {
        this.checksum = checksum;
        return this;
    }

    public List<DatabaseChange> getChanges() {
        return changes;
    }

    public DatabaseChangeSet setChanges(List<DatabaseChange> changes) {
        this.changes = changes;
        return this;
    }

    public int getSort() {
        return sort;
    }

    public DatabaseChangeSet setSort(int sort) {
        this.sort = sort;
        return this;
    }

    public Revision getRevision() {
        return revision;
    }

    public DatabaseChangeSet setRevision(Revision revision) {
        this.revision = revision;
        return this;
    }
}
