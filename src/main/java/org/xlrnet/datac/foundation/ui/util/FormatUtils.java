package org.xlrnet.datac.foundation.ui.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.vcs.domain.Revision;

/**
 * Utility class with helper methods for formatting stuff.
 */
public class FormatUtils {

    /**
     * The amount of characters to display in the revision's internal id.
     */
    private static final int REVISION_ABBREVIATION_LENGTH = 7;

    private FormatUtils() {

    }

    @NotNull
    public static String formatRevisionWithMessage(@NotNull Revision firstRevision) {
        return StringUtils.substring(firstRevision.getInternalId(), 0, REVISION_ABBREVIATION_LENGTH) + " - " + StringUtils.substringBefore(firstRevision.getMessage(), "\n");
    }

    @NotNull
    public static String abbreviateRevisionId(@NotNull Revision revision) {
        return StringUtils.substring(revision.getInternalId(), 0, REVISION_ABBREVIATION_LENGTH);
    }
}
