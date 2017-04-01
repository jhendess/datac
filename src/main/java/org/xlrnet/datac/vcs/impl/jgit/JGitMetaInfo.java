package org.xlrnet.datac.vcs.impl.jgit;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.vcs.api.VcsMetaInfo;

/**
 * Meta info data for git using JGit.
 */
public class JGitMetaInfo implements VcsMetaInfo {

    @NotNull
    @Override
    public String getVcsName() {
        return "git";
    }

    @NotNull
    @Override
    public String getAdapterName() {
        return "JGit";
    }
}
