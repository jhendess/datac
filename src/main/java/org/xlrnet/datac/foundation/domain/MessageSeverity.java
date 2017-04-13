package org.xlrnet.datac.foundation.domain;

/**
 * Severity level of a single event message.
 */
public enum MessageSeverity {
    /** Simple information message. */
    INFO(1),
    /** Warning message for an unusual state. */
    WARNING(2),
    /** Critical error occurred. */
    ERROR(3);

    private final int severity;

    private static int HIGHEST_SEVERITY;

    MessageSeverity(int severity) {
        this.severity = severity;
    }

    public int getSeverityLevel() {
        return severity;
    }

    public static int getHighestSeverityLevel() {
        if (HIGHEST_SEVERITY == -1) {
            int tmpSeverity = 0;
            for (MessageSeverity messageSeverity : values()) {
                if (messageSeverity.getSeverityLevel() > tmpSeverity) {
                    tmpSeverity = messageSeverity.getSeverityLevel();
                }
            }
            HIGHEST_SEVERITY = tmpSeverity;
        }
        return HIGHEST_SEVERITY;
    }
}
