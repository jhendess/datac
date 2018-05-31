package org.xlrnet.datac.database.util;

/**
 * Phases of a deployment process.
 */
public enum DeploymentPhase {

    LOCKING,

    CONFIG_VALIDATION,

    CONNECTION_VALIDATION,

    SQL_GENERATION,

    MIGRATION,

    POST_PROCESS;
}
