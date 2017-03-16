package org.xlrnet.datac.foundation.configuration;

/**
 * Class containing static constants for startup phases in the application. Classes implementing {@link
 * org.springframework.context.Phased} may use these constants to boot in a correct order.
 */
public final class StartupPhases {

    private StartupPhases() {
        // No instances allowed
    }

    /**
     * Initial startup phase which performs internal validation and preparation.
     */
    public static int PREPARATION = 1;

    /**
     * Infrastructure is prepared. Application can continue with internal configurations.
     */
    public static int CONFIGURATION = 2;

    /**
     * Application is configured. Concrete business logic may now run.
     */
    public static int READY = 3;
}
