package org.xlrnet.datac.foundation.domain;

/**
 * Type of events that can be logged.
 */
public enum EventType {

    /** A project was updated. */
    PROJECT_UPDATE,
    /** An uncaught exception was thrown. */
    UNCAUGHT_EXCEPTION,
    /** Application startup. */
    STARTUP,
    /** Project is being cleaned. */
    PROJECT_CLEANUP,
    /** Change sets are reset manually. */
    CHANGESET_RESET;
}
