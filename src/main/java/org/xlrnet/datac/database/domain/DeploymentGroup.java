package org.xlrnet.datac.database.domain;

import static com.google.common.base.Preconditions.checkState;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.domain.Branch;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Defines a group of deployable databases.
 */
@Entity
@ToString(exclude = {"children", "parent", "instances"})
@NoArgsConstructor
@Table(name = "db_group")
@EqualsAndHashCode(callSuper = true, exclude = {"project", "children", "parent", "instances"})
public class DeploymentGroup extends AbstractDeploymentInstance implements IDatabaseInstance {

    /**
     * Project to which this group belongs
     */
    @Setter
    @Getter
    @NotNull
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    /** Parent group. */
    @Getter
    @Setter
    @ManyToOne
    private DeploymentGroup parent;

    /** Child groups. */
    @Valid
    @Getter
    @OneToMany(targetEntity = DeploymentGroup.class, cascade = CascadeType.ALL)
    @JoinColumn(name = "parent_id")
    private Set<DeploymentGroup> children = new HashSet<>();

    /** Instances in this deployment group. */
    @Valid
    @Getter
    @OneToMany(mappedBy = "group", targetEntity = DeploymentInstance.class, fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    private Set<DeploymentInstance> instances = new HashSet<>();

    public DeploymentGroup(String name, Project project) {
        this(name, project, null);
    }

    public DeploymentGroup(String name, Project project, Branch branch) {
        setName(name);
        setBranch(branch);
        this.project = project;
    }

    public DeploymentGroup(Project project, DeploymentGroup parent) {
        this.project = project;
        this.parent = parent;
    }

    public void addChildGroup(@NotNull DeploymentGroup childGroup) {
        if (!getChildren().contains(childGroup)) {
            checkState(childGroup.getParent() == null, "New child group may not be child of another group");
            childGroup.setParent(this);
            children.add(childGroup);
        }
    }

    public void addInstance(@NotNull DeploymentInstance instance) {
        if (!getInstances().contains(instance)) {
            checkState(instance.getGroup() == null, "New instance may not be child of another group");
            instance.setGroup(this);
            instances.add(instance);
        }
    }

    @org.jetbrains.annotations.NotNull
    public String getParentPath() {
        if (getParent() == null) {
            return "";
        } else {
            String parentPath = getParent().getParentPath();
            String parentName = getParent().getName();
            return parentPath + "/" + parentName;
        }
    }

    @Override
    public InstanceType getInstanceType() {
        return InstanceType.GROUP;
    }
}
