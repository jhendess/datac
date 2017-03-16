package org.xlrnet.datac.vcs.domain;

import com.google.common.base.MoreObjects;
import org.xlrnet.datac.foundation.domain.AbstractEntity;
import org.xlrnet.datac.foundation.domain.Project;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Objects;

/**
 * Representation of a branch in a VCS.
 */
@Entity
@Table(name = "branch")
public class Branch extends AbstractEntity {

    @NotNull
    @Size(max = 50)
    @Column(name = "name")
    private String name;

    @NotNull
    @Size(max = 256)
    @Column(name = "internalId")
    private String internalId;

    @Column(name = "watched")
    private boolean watched;

    @Column(name = "development")
    private boolean development;

    @ManyToOne(targetEntity = Project.class)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    public void setName(String name) {
        this.name = name;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    /**
     * Returns the humanly readable name of the branch. This is usually the value that is also displayed to the user.
     *
     * @return the humanly readable name of the branch.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the internal id of this branch. This is usually a technical checksum that points to a revision in the
     * VCS.
     *
     * @return the internal id of this branch.
     */
    public String getInternalId() {
        return this.internalId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("internalId", internalId)
                .add("watched", watched)
                .toString();
    }

    /**
     * Returns whether the branch should be watched for changes.
     *
     * @return True if the branch should be watched or false if not.
     */
    public boolean isWatched() {
        return watched;
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
        project.addBranch(this);
    }

    public boolean isDevelopment() {
        return development;
    }

    public void setDevelopment(boolean development) {
        this.development = development;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Branch branch = (Branch) o;
        return watched == branch.watched &&
                Objects.equals(name, branch.name) &&
                Objects.equals(internalId, branch.internalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, internalId, watched);
    }
}