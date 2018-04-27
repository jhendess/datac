package org.xlrnet.datac.commons.util;

/**
 * Custom preconditions for easier checks. All checks throw runtime exceptions if a given condition is not met.
 */
public class Preconditions {

    private Preconditions() {
        
    }

    /**
     * Checks if the given condition is true. If not, a new {@link ConfigurationException} will be raised.
     * @param condition The condition to check.
     * @param messageTemplate The message template to output. Uses String.format().
     * @param parameters The parameters for the template.
     */
    public static void checkConfiguration(boolean condition, String messageTemplate, Object... parameters ) {
        if (!condition) {
            throw new ConfigurationException(String.format(messageTemplate, parameters));
        }
    }
}
