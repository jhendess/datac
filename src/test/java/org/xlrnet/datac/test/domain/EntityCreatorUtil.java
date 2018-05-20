package org.xlrnet.datac.test.domain;

import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.database.domain.DatabaseConnection;
import org.xlrnet.datac.database.domain.DatabaseType;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.ProjectState;
import org.xlrnet.datac.vcs.domain.Branch;

/**
 * Test utilities for creating entities.
 */
public class EntityCreatorUtil {

    private EntityCreatorUtil() {
        // Private constructor
    }

    @NotNull
    public static Branch buildBranch() {
        Branch testBranch = new Branch();
        testBranch.setName(RandomStringUtils.randomAlphabetic(10));
        testBranch.setDevelopment(true);
        testBranch.setInternalId("1");
        return testBranch;
    }

    public static Project buildProject() {
        Project p = new Project();
        p.setChangelogLocation("/");
        p.setVcsAdapterClass("TEST");
        p.setVcsType("TEST");
        p.setUrl("Some_URL");
        p.setNewBranchPattern(".*");
        p.setName("TEST");
        p.setChangeSystemAdapterClass("TEST");
        p.setState(ProjectState.NEW);
        return p;
    }

    public static DatabaseConnection buildDatabaseConnection(String name) {
        DatabaseConnection databaseConnection = new DatabaseConnection();
        databaseConnection.setName(name);
        databaseConnection.setType(DatabaseType.H2);
        databaseConnection.setJdbcUrl("jdbc:h2:mem:foo");
        databaseConnection.setUser("test");
        databaseConnection.setPassword("foobar");
        return databaseConnection;
    }
}
