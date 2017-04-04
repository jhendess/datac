package org.xlrnet.datac.foundation.domain;

/**
 * Severity level of a single event message.
 */
public enum MessageSeverity {
    /** Simple information message. */
    INFO,
    /** Warning message for an unusual state. */
    WARNING,
    /** Critical error occurred. */
    ERROR;
}
