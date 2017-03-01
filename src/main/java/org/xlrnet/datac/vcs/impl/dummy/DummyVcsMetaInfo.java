package org.xlrnet.datac.vcs.impl.dummy;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.vcs.api.VcsMetaInfo;

/**
 * Dummy implementation of {@link VcsMetaInfo}
 */
public class DummyVcsMetaInfo implements VcsMetaInfo {

    @NotNull
    @Override
    public String getVcsName() {
        return "Dummy";
    }

    @NotNull
    @Override
    public String getAdapterName() {
        return "Dummy adapter";
    }
}
