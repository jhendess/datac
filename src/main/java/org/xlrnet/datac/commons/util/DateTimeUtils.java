package org.xlrnet.datac.commons.util;

import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * Date and time utilities.
 */
public class DateTimeUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtils() {
        // No instances allowed
    }

    /**
     * Returns a thread-safe date and time formatter with the default pattern.
     *
     * @return a thread-safe date and time formatter with the default pattern.
     */
    @NotNull
    public static DateTimeFormatter getDefaultDateTimeFormatter() {
        return DATE_TIME_FORMATTER;
    }

    /**
     * Formats the given temporal with the default formatter.
     *
     * @param temporal
     *         The object to format.
     * @return A formatted string of the given temporal.
     */
    @NotNull
    public static String format(@NotNull TemporalAccessor temporal) {
        return getDefaultDateTimeFormatter().format(temporal);
    }

}
