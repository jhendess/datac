package org.xlrnet.datac.database.tasks;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.commons.tasks.AbstractRunnableTask;
import org.xlrnet.datac.database.domain.ConnectionPingResult;
import org.xlrnet.datac.database.domain.DatabaseConnection;
import org.xlrnet.datac.database.services.ConnectionManagerService;

/**
 * Task which checks whether a database connection can be established.
 */
public class CheckDatabaseConnectionTask extends AbstractRunnableTask<ConnectionPingResult> {

    /** The connection config to use for pinging. */
    private final DatabaseConnection connectionToCheck;

    /** The connection manager to use for checking the connection. */
    private final ConnectionManagerService connectionManager;

    public CheckDatabaseConnectionTask(@NotNull DatabaseConnection connectionToCheck, @NotNull ConnectionManagerService connectionManager) {
        this.connectionToCheck = connectionToCheck;
        this.connectionManager = connectionManager;
    }

    @Override
    protected void runTask() {
        ConnectionPingResult connectionPingResult = connectionManager.pingConnection(connectionToCheck);
        getEntityChangeHandler().onChange(connectionPingResult);
    }
}
