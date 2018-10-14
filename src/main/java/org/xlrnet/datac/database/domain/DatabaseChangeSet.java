package org.xlrnet.datac.database.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;
import org.xlrnet.datac.foundation.domain.AbstractEntity;
import org.xlrnet.datac.foundation.domain.Sortable;
import org.xlrnet.datac.foundation.domain.validation.Sorted;
import org.xlrnet.datac.vcs.domain.Revision;

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
     * The name of the file which contains the changeset.
     */
    @NotEmpty
    @Size(max = 1024)
    @Column(name = "source_filename")
    private String sourceFilename;

    /**
     * The revision to which this change set belongs.
     */
    @JoinColumn(name = "revision_id")
    @OneToOne(targetEntity = Revision.class, cascade = {CascadeType.REFRESH}, fetch = FetchType.EAGER)
    private Revision revision;

    /**
     * The change set where this change set was first encountered.
     */
    @JoinColumn(name = "introducing_changeset_id")
    @OneToOne(targetEntity = DatabaseChangeSet.class, cascade = {CascadeType.REFRESH}, fetch = FetchType.EAGER)
    private DatabaseChangeSet introducingChangeSet;

    /**
     * Flag to indicate if this change set conflicts (i.e. modifies) with its introducing change set.
     */
    @Column(name = "modifying")
    private boolean modifying;

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

    public DatabaseChangeSet getIntroducingChangeSet() {
        return introducingChangeSet;
    }

    public DatabaseChangeSet setIntroducingChangeSet(DatabaseChangeSet introducingRevision) {
        this.introducingChangeSet = introducingRevision;
        return this;
    }

    public boolean isModifying() {
        return modifying;
    }

    public void setModifying(boolean modifying) {
        this.modifying = modifying;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public DatabaseChangeSet setSourceFilename(String sourceFilename) {
        this.sourceFilename = sourceFilename;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatabaseChangeSet)) return false;
        DatabaseChangeSet that = (DatabaseChangeSet) o;
        return sort == that.sort &&
                Objects.equals(internalId, that.internalId) &&
                Objects.equals(comment, that.comment) &&
                Objects.equals(author, that.author) &&
                Objects.equals(checksum, that.checksum) &&
                Objects.equals(modifying, that.modifying) &&
                Objects.equals(sourceFilename, that.sourceFilename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(internalId, comment, author, checksum, sort, modifying, sourceFilename);
    }
}
