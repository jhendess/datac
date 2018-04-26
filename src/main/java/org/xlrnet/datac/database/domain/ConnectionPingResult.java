package org.xlrnet.datac.database.domain;

import java.sql.SQLException;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Small value class with metadata about a database connection. Metadata is only available if the connection could be
 * established successfully.
 */
@Value
@AllArgsConstructor
public class ConnectionPingResult {

    /** The JDBC URL that was used for connecting. */
    private String jdbcUrl;

    /** Flag to determine whether a connection could be established. */
    private boolean connected;

    /** Name of the connected db product. Null if not connected. */
    private String dbProductName;

    /** Version of the connected db product. Null if not connected. */
    private String dbProductVersion;

    /** Exception which might be thrown in case of a failed connection attempt. Null if connected. */
    private SQLException exception;

    public ConnectionPingResult(String jdbcUrl, boolean connected, SQLException exception) {
        this.jdbcUrl = jdbcUrl;
        this.connected = connected;
        this.dbProductName = null;
        this.dbProductVersion = null;
        this.exception = exception;
    }
}
