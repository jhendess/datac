package org.xlrnet.datac.database.impl.liquibase;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.database.api.DatabaseChangeSystemMetaInfo;

/**
 * Meta info for the liquibase adapter.
 */
public class LiquibaseAdapterMetaInfo implements DatabaseChangeSystemMetaInfo{

    private static final LiquibaseAdapterMetaInfo INSTANCE = new LiquibaseAdapterMetaInfo();

    public static LiquibaseAdapterMetaInfo getInstance() {
        return INSTANCE;
    }

    private LiquibaseAdapterMetaInfo() {
        // Only one instance possible
    }

    @NotNull
    @Override
    public String getAdapterName() {
        return "Liquibase";
    }
}
