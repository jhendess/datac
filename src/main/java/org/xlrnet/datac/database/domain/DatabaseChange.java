package org.xlrnet.datac.database.domain;

import org.hibernate.validator.constraints.NotEmpty;
import org.xlrnet.datac.foundation.domain.AbstractEntity;
import org.xlrnet.datac.foundation.domain.Sortable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * A single database change that is executed as part of a {@link DatabaseChangeSet}.
 */
@Entity
@Table(name = "change")
public class DatabaseChange extends AbstractEntity implements Sortable {

    /**
     * Name of the change (?).
     */
    @NotEmpty
    @Size(max = 256)
    @Column(name = "type")
    private String type;

    /**
     * Description of the database change.
     */
    @Column(name = "description")
    private String description;

    /**
     * Preview SQL generated for a generic database (if generation was possible).
     */
    @Column(name = "preview_sql")
    private String previewSql;

    /**
     * Checksum of this change.
     */
    @NotEmpty
    @Size(max = 256)
    @Column(name = "checksum")
    private String checksum;

    /**
     * Integer to sort by. Defines in which order the change must be executed.
     */
    @Min(0)
    @Column(name = "sort")
    private int sort;

    public String getDescription() {
        return description;
    }

    public DatabaseChange setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getType() {
        return type;
    }

    public DatabaseChange setType(String type) {
        this.type = type;
        return this;
    }

    public String getPreviewSql() {
        return previewSql;
    }

    public DatabaseChange setPreviewSql(String previewSql) {
        this.previewSql = previewSql;
        return this;
    }

    public String getChecksum() {
        return checksum;
    }

    public DatabaseChange setChecksum(String checksum) {
        this.checksum = checksum;
        return this;
    }

    @Override
    public int getSort() {
        return sort;
    }

    public DatabaseChange setSort(int sort) {
        this.sort = sort;
        return this;
    }
}
