package org.xlrnet.datac.vcs.api;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A revision in a VCS describes a single versioning state. Revisions are always chained and resemble a graph.
 */
public interface VcsRevision {

    /**
     * Returns the internal id of the revision. This may e.g. be a sequential numbered value or some kind of checksum.
     *
     * @return the internal id of the revision.
     */
    @NotNull
    @NotEmpty
    @Size(max = 256)
    String getInternalId();

    /**
     * Returns a message which describes the changes in this revision.
     *
     * @return a message which describes the changes in this revision.
     */
    String getMessage();

    /**
     * Returns the name of the author who originally created the revision in the VCS.
     *
     * @return the name of the author who originally created the revision in the VCS.
     */
    @Nullable
    @Size(max = 256)
    String getAuthor();

    /**
     * Returns the name of the user who submitted the reviewed revision to the VCS.
     *
     * @return the name of the user who submitted the reviewed revision to the VCS.
     */
    @Nullable
    @Size(max = 256)
    String getReviewer();

    /**
     * Returns the time when the revision was originally created.
     *
     * @return the time when the revision was originally created.
     */
    LocalDateTime getCommitTime();

    /**
     * Lists all direct parents of this revision. If there are no parents, an empty list must be returned.
     *
     * @return all direct parents of this revision.
     */
    @NotNull
    List<? extends VcsRevision> getParents();
}
