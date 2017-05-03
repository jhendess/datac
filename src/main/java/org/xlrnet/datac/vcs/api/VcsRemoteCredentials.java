package org.xlrnet.datac.vcs.api;

import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.NotNull;

/**
 * Credentials for connecting to a remote VCS.
 */
public interface VcsRemoteCredentials {

    @NotNull
    public String getUrl();

    @Nullable
    public String getUsername();

    @Nullable
    public String getPassword();
}
