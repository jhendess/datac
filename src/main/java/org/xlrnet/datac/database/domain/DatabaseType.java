package org.xlrnet.datac.database.domain;

/**
 * Enum which contains a list of all supported database systems.
 */
public enum DatabaseType {

    /** H2 database server. */
    H2("org.h2.Driver", "");

    private final String driverClassName;

    private final String connectionTestSql;

    DatabaseType(String driverClassName, String connectionTestSql) {
        this.driverClassName = driverClassName;
        this.connectionTestSql = connectionTestSql;
    }
}
