package org.xlrnet.datac.database.util;

import liquibase.resource.FileSystemResourceAccessor;

/**
 * Custom implementation of liquibase's {@link FileSystemResourceAccessor}.
 */
public class CustomLiquibaseFileSystemResourceAccessor extends FileSystemResourceAccessor {

    public CustomLiquibaseFileSystemResourceAccessor() {
    }

    public CustomLiquibaseFileSystemResourceAccessor(String base) {
        super(base);
    }

    @Override
    protected String convertToPath(String string) {
        return string;
    }
}
