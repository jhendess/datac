package org.xlrnet.datac.database.domain;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;
import org.xlrnet.datac.foundation.domain.AbstractEntity;

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
public class DeploymentInstance extends AbstractEntity implements IDatabaseInstance, Comparable<DeploymentInstance> {

    /** Name of the database instance. */
    @Setter
    @Getter
    @NotEmpty
    @Size(max = 50)
    @Column(name = "name")
    private String name;

    /** Group in which this instance is deployed. */
    @Setter
    @Getter
    @NotNull
    @OneToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "group_id")
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
        this.name = name;
        this.connection = connection;
    }

    public DeploymentInstance(String name, DatabaseConnection connection, DeploymentGroup group) {
        this.name = name;
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
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public int compareTo(@org.jetbrains.annotations.NotNull DeploymentInstance o) {
        return this.getName().compareTo(o.getName());
    }
}
