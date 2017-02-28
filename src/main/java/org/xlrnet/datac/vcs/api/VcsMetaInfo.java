package org.xlrnet.datac.vcs.api;

import org.jetbrains.annotations.NotNull;

/**
 * Metadata for a version control system.
 */
public interface VcsMetaInfo {

    /**
     * Returns the name of the VCS supported by the adapter. This may be e.g. something like "git" for git repositories.
     *
     * @return the name of the VCS supported by the adapter.
     */
    @NotNull
    String getVcsName();

    /**
     * Returns the custom name of the VCS adapter. This doesn't have to correlate with the actual used VCS, but may be
     * any artificial name.
     *
     * @return the custom name of the VCS adapter.
     */
    @NotNull
    String getAdapterName();
}
