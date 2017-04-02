package org.xlrnet.datac.vcs.impl.jgit;

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.exception.VcsRepositoryException;
import org.xlrnet.datac.vcs.api.*;

import java.nio.file.Path;

/**
 * {@link org.xlrnet.datac.vcs.api.VcsAdapter} implementation for git using Eclipse JGit.
 */
@Component
public class JGitAdapter implements VcsAdapter {

    @NotNull
    @Override
    public VcsMetaInfo getMetaInfo() {
        return new JGitMetaInfo();
    }

    @NotNull
    @Override
    public VcsRemoteRepositoryConnection connectRemote(@NotNull VcsRemoteCredentials credentials) throws VcsConnectionException {
        UsernamePasswordCredentialsProvider provider = buildCredentialsProvider(credentials);
        return new JGitRemoteRepositoryConnection(credentials.getUrl(), provider);
    }

    @NotNull
    @Override
    public VcsLocalRepository openLocalRepository(@NotNull Path repositoryPath, @NotNull VcsRemoteCredentials credentials) throws VcsRepositoryException {
        UsernamePasswordCredentialsProvider provider = buildCredentialsProvider(credentials);
        return new JGitLocalRepository(repositoryPath, provider, credentials.getUrl());
    }

    @NotNull
    private UsernamePasswordCredentialsProvider buildCredentialsProvider(@NotNull VcsRemoteCredentials credentials) {
        char[] password = credentials.getPassword() != null ? credentials.getPassword().toCharArray() : new char[0];
        return new UsernamePasswordCredentialsProvider(credentials.getUsername(), password);
    }
}
