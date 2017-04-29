package org.xlrnet.datac.commons.util;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.xlrnet.datac.foundation.domain.MessageSeverity;

/**
 * Utilities for displaying UI stuff.
 */
public class DisplayUtils {

    private static final String NEWLINE = "<br>";

    private static final String TAB_REPLACEMENT = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";

    private DisplayUtils() {
        // No instances allowed
    }


    /**
     * Converts and sanitizes given input string to HTML. Performs the following operations:
     * <ul>
     * <li>Newline becomes a br-tag</li>
     * <li>Tab becomes whitespace</li>
     * <li>Html entities are being replaced</li>
     * </ul>
     *
     * @param inputString
     *         The input string to convert.
     * @return A sanitized HTML representation of the given input.
     */
    public static String convertToHtml(String inputString) {
        String escaped = StringEscapeUtils.escapeHtml4(inputString);
        escaped = StringUtils.replace(escaped, "\t", TAB_REPLACEMENT);
        return StringUtils.replace(escaped, "\n", NEWLINE);
    }

    /**
     * Converts a given {@link MessageSeverity} to the appropriate CSS style.
     *
     * @param severity
     *         The message severity to convert.
     * @return The CSS style.
     */
    public static String severityToStyle(MessageSeverity severity) {
        String style = null;
        switch (severity) {
            case ERROR:
                style = "severity-error";
                break;
            case WARNING:
                style = "severity-warning";
                break;
        }
        return style;
    }
}
