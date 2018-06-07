package org.xlrnet.datac.database.domain;

import lombok.Getter;

/**
 * Enum which contains a list of all supported database systems.
 */
public enum DatabaseType {

    /** H2 database server. */
    H2("org.h2.Driver", ""),

    /** HSQL database server. */
    HSQL("org.hsqldb.jdbc.JDBCDriver", "");

    @Getter
    private final String driverClassName;

    @Getter
    private final String connectionTestSql;

    DatabaseType(String driverClassName, String connectionTestSql) {
        this.driverClassName = driverClassName;
        this.connectionTestSql = connectionTestSql;
    }
}
