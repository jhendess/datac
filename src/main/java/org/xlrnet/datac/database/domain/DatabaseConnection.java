package org.xlrnet.datac.database.domain;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;
import org.xlrnet.datac.foundation.domain.AbstractEntity;
import org.xlrnet.datac.foundation.domain.PasswordEncryptedEntity;
import org.xlrnet.datac.foundation.domain.PasswordEncryptionListener;

import lombok.Data;

/**
 * Configuration object for a single database connection.
 */
@Data
@Entity
@Table(name = "db_connection")
@EntityListeners(PasswordEncryptionListener.class)
public class DatabaseConnection extends AbstractEntity implements PasswordEncryptedEntity {

    /**
     * Name of the database connection configuration.
     */
    @NotEmpty
    @Size(max = 50)
    @Column(name = "name")
    private String name;

    /**
     * Type of the database that will be connected to.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private DatabaseType type;

    /**
     * Database host to which should be connected.
     */
    @Size(max = 50)
    @Column(name = "host")
    private String host;

    /**
     * Database port to which should be connected.
     */
    @Max(65535)
    @Column(name = "port")
    private Integer port;

    /**
     * Salt used for encrypting the user credentials. Will be automatically set by an entity listener.
     */
    @NotNull
    @Column(name = "salt")
    private byte[] salt;

    /**
     * Login user for this connection.
     */
    @NotEmpty
    @Size(max = 20)
    private String user;

    /**
     * Encrypted password as base64.
     */
    @Column(name = "password")
    private String encryptedPassword;

    /**
     * Unencrypted password representation.
     */
    @Transient
    @Size(max = 50)
    private String password;

    /**
     * The schema to use in the database.
     */
    @Size(max = 20)
    @Column(name = "schema")
    private String schema;

    /**
     * The full JDBC database url. May either be automatically generated or manually entered.
     */
    @NotEmpty
    @Size(max = 200)
    @Column(name = "url")
    private String jdbcUrl;

    /** Inverse relationship of the associated instance. Not fetched by default. */
    @OneToOne(optional = true, mappedBy = "connection", fetch = FetchType.LAZY)
    private DeploymentInstance instance;

}
