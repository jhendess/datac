package org.xlrnet.datac.database.impl.liquibase;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.stereotype.Component;
import org.xlrnet.datac.database.domain.DatabaseConnection;
import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.database.services.ConnectionManagerService;

import liquibase.database.jvm.HsqlConnection;
import liquibase.database.jvm.JdbcConnection;

/**
 * Factory for creating liquibase {@link liquibase.database.DatabaseConnection} instances.
 */
@Component
public class LiquibaseConnectionFactory {

    /** Service for obtaining JDBC connections. */
    private final ConnectionManagerService connectionManagerService;

    public LiquibaseConnectionFactory(ConnectionManagerService connectionManagerService) {
        this.connectionManagerService = connectionManagerService;
    }

    /**
     * Create a new {@link DatabaseConnection} to the given deployment instance. This method establishes and opens a
     * new JDBC connection. Make sure to close the connection manually to avoid leaking connections.
     * @param instance  The config to use for establishing the connection.
     * @return
     * @throws SQLException Thrown in case of an error.
     */
    public liquibase.database.DatabaseConnection createDatabaseConnectionFromConfig(DeploymentInstance instance) throws SQLException {
        DatabaseConnection connection = instance.getConnection();
        liquibase.database.DatabaseConnection liquibaseConnection = null;

        Connection jdbcConnection = connectionManagerService.getConnectionFromConfig(connection, true);

        switch (connection.getType()) {
            case HSQL:
                liquibaseConnection = new HsqlConnection(jdbcConnection);
                break;
            default:
                liquibaseConnection = new JdbcConnection(jdbcConnection);
        }
        return liquibaseConnection;
    }
}
