package org.xlrnet.datac.database.services;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.database.domain.ConnectionPingResult;
import org.xlrnet.datac.database.domain.DatabaseConnection;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for establishing JDBC connections and performing basic operations on them (e.g. pinging).
 */
@Slf4j
@Service
public class ConnectionManagerService {

    /**
     * Establishes a JDBC connection using the given {@link DatabaseConnection} for configuration.
     * @param config The entity to use for deriving the connection.
     * @param useSchema If set to true, then the schema defined in the entity will be loaded (if it isn't blank).
     * @return A JDBC connection.
     * @throws SQLException if a database access error occurs or the url is null
     */
    private Connection getConnectionFromEntity(DatabaseConnection config, boolean useSchema) throws SQLException {
        Connection connection = DriverManager.getConnection(config.getJdbcUrl(), config.getUser(), config.getPassword());
        if (useSchema && StringUtils.isNotBlank(config.getSchema())) {
            connection.setSchema(config.getSchema());
        }
        return connection;
    }

    /**
     * Tries to ping the JDBC database defined by the given {@link DatabaseConnection} object and returns information
     * about the connection.
     * @param config The configuration to use for establishing the connection.
     * @return The connection result.
     */
    public ConnectionPingResult pingConnection(DatabaseConnection config) {
        ConnectionPingResult connectionPingResult;
        String jdbcUrl = config.getJdbcUrl();
        LOGGER.info("Pinging connection to {}", jdbcUrl);
        try (Connection connection = getConnectionFromEntity(config, true)) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbProductName = metaData.getDatabaseProductName();
            String dbProductVersion = metaData.getDatabaseProductVersion();
            LOGGER.info("Successfully connected to {} {} via {}", dbProductName, dbProductVersion, jdbcUrl);
            connectionPingResult = new ConnectionPingResult(jdbcUrl, true, dbProductName, dbProductVersion, null);
        } catch (SQLException e) {
            LOGGER.error("Connection test to {} failed", jdbcUrl, e);
            connectionPingResult = new ConnectionPingResult(jdbcUrl, false, e);
        }
        return connectionPingResult;
    }
}
