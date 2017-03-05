package org.xlrnet.datac.vcs.api;

import javax.validation.constraints.NotNull;

import org.jetbrains.annotations.Nullable;

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
