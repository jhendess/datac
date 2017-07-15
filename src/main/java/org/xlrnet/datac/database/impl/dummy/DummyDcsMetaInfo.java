package org.xlrnet.datac.database.impl.dummy;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.database.api.DatabaseChangeSystemMetaInfo;

/**
 * Dummy implementation of {@link DatabaseChangeSystemMetaInfo}.
 */
public class DummyDcsMetaInfo implements DatabaseChangeSystemMetaInfo {

    @NotNull
    @Override
    public String getAdapterName() {
        return "Dummy";
    }
}
