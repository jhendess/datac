package org.xlrnet.datac.foundation.ui.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.vcs.domain.Revision;

/**
 * Utility class with helper methods for formatting stuff.
 */
@Service
public class RevisionFormatService {

    /**
     * The amount of characters to display in the revision's internal id.
     */
    private static final int REVISION_ABBREVIATION_LENGTH = 7;

    /**
     * The amount of characters to display in a revision's message.
     */
    private static final int MESSAGE_ABBREVIATION_LENGTH = 80;

    /**
     * Default date time formatter.
     */
    private DateTimeFormatter defaultDateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());

    @NotNull
    public String formatMessage(@NotNull Revision rev) {
        return StringUtils.abbreviate(StringUtils.substringBefore(rev.getMessage(), "\n"), MESSAGE_ABBREVIATION_LENGTH);
    }

    @NotNull
    public String formatTimestamp(@NotNull Revision revision) {
        return defaultDateTimeFormatter.format(revision.getCommitTime());
    }

    @NotNull
    public String formatAuthor(@NotNull Revision revision) {
        return StringUtils.substringBefore(revision.getAuthor(), "<");  // TODO: Refine logic to extract the author
    }

    @NotNull
    public String formatRevisionWithMessage(@NotNull Revision firstRevision) {
        return StringUtils.substring(firstRevision.getInternalId(), 0, REVISION_ABBREVIATION_LENGTH) + " - " + StringUtils.substringBefore(firstRevision.getMessage(), "\n");
    }

    @NotNull
    public String abbreviateRevisionId(@NotNull Revision revision) {
        return StringUtils.substring(revision.getInternalId(), 0, REVISION_ABBREVIATION_LENGTH);
    }
}
