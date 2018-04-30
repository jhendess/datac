package org.xlrnet.datac.database.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import org.xlrnet.datac.foundation.domain.AbstractEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Size;

/**
 * Concrete instance of a single deployment. Instances always use a configured {@link DatabaseConnection} in order to
 * connect with a remote DBMS. If an instance is part of a group, it can inherit certain properties of the group (i.e. deployed branch).
 */
@Entity
@ToString
@NoArgsConstructor
@Table(name = "db_instance")
@EqualsAndHashCode(callSuper = true, exclude = "group")
public class DeploymentInstance extends AbstractEntity {

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
    @OneToOne
    @JoinColumn(name = "group_id")
    private DeploymentGroup group;

    /** The connection which is configured for this instance. */
    @Setter
    @Getter
    @OneToOne(optional = false)
    @JoinColumn(name = "connection_id")
    private DatabaseConnection connection;

    public DeploymentInstance(String name, DatabaseConnection connection) {
        this.name = name;
        this.connection = connection;
    }
}
