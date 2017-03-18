package org.xlrnet.datac.vcs.api;

import org.jetbrains.annotations.NotNull;

/**
 * A revision in a VCS describes a single versioning state. Revisions are always chained and resemble a graph.
 */
public interface VcsRevision {

    /**
     * Returns the internal id of the revision. This may e.g. be some kind of unique checksum or a simple incrementing
     * counter.
     *
     * @return The internal id of the revision.
     */
    @NotNull
    String getInternalId();

    /**
     * Returns a message of the revision which describes the changes done since the parent.
     *
     * @return
     */
    @NotNull
    String getRevisionMessage();

}
