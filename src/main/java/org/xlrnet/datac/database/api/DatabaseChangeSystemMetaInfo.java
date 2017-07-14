package org.xlrnet.datac.database.api;

import org.jetbrains.annotations.NotNull;

/**
 * Metadata for a database change system adapter. The concrete implementation of this class should be either a singleton or
 * equal to any other object of the concrete implementation.
 */
public interface DatabaseChangeSystemMetaInfo {

    /**
     * Returns the custom name of the database change adapter. This doesn't have to correlate with the actual used VCS, but may be
     * any artificial name.
     *
     * @return the custom name of the VCS adapter.
     */
    @NotNull
    String getAdapterName();
}
