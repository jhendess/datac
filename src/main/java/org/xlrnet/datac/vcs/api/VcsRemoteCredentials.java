package org.xlrnet.datac.vcs.api;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Credentials for connecting to a remote VCS.
 */
public class VcsRemoteCredentials {

    @NotEmpty
    private String url;

    @NotEmpty
    private String username;

    @NotEmpty
    private String password;

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
