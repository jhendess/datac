package org.xlrnet.datac.database.domain;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.xlrnet.datac.vcs.domain.Branch;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Concrete instance of a single deployment. Instances always use a configured {@link DatabaseConnection} in order to
 * connect with a remote DBMS. If an instance is part of a group, it can inherit certain properties of the group (i.e. deployed branch).
 */
@Entity
@NoArgsConstructor
@Table(name = "db_instance")
@EqualsAndHashCode(callSuper = true, exclude = {"group", "connection"})
public class DeploymentInstance extends AbstractDeploymentInstance {

    /** Group in which this instance is deployed. */
    @Setter
    @Getter
    @NotNull
    @JoinColumn(name = "group_id")
    @OneToOne(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    private DeploymentGroup group;

    /** The connection which is configured for this instance. */
    @Setter
    @Getter
    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(name = "connection_id")
    private DatabaseConnection connection;

    public DeploymentInstance(DeploymentGroup group) {
        this.group = group;
    }

    public DeploymentInstance(String name, DatabaseConnection connection) {
        setName(name);
        this.connection = connection;
    }

    public DeploymentInstance(String name, DatabaseConnection connection, DeploymentGroup group) {
        setName(name);
        this.connection = connection;
        this.group = group;
    }

    @Override
    public InstanceType getInstanceType() {
        return InstanceType.DATABASE;
    }

    @Override
    public String toString() {
        return "DeploymentInstance{" +
                "name='" + getName() + '\'' +
                '}';
    }

    @Override
    DeploymentGroup getParent() {
        return getGroup();
    }

    // Must be not-null for instances
    @Override
    @NotNull(message = "Branch must be either defined on instance or inherited from group")
    public Branch getActualBranch() {
        return super.getActualBranch();
    }

    public String getFullPath() {
        return getParent().getParentPath() + "/" + getParent().getName() + "/" + getName();
    }
}
