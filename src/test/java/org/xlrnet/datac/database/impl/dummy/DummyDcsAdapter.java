package org.xlrnet.datac.database.impl.dummy;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.database.api.DatabaseChangeSystemAdapter;
import org.xlrnet.datac.database.api.DatabaseChangeSystemMetaInfo;
import org.xlrnet.datac.database.domain.DatabaseChange;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.foundation.domain.Project;

/**
 * Dummy change adapter which corresponds to the dummy VCS adapter. Creates a complex change graph with various
 * conflicts.
 */
@Component
public class DummyDcsAdapter implements DatabaseChangeSystemAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummyDcsAdapter.class);

    private static final String CHECKSUM_A_ORIGINAL = RandomStringUtils.random(16);

    private static final String CHECKSUM_A_MODIFIED_4 = RandomStringUtils.random(16);

    private static final String CHECKSUM_A_MODIFIED_5 = RandomStringUtils.random(16);

    private static final String CHECKSUM_B_ORIGINAL = RandomStringUtils.random(16);

    private static final String CHECKSUM_C_ORIGINAL = RandomStringUtils.random(16);

    private static final String CHECKSUM_C_MODIFIED = RandomStringUtils.random(16);

    public static int CHECKED_OUT_REVISION = 6;

    @NotNull
    @Override
    public DatabaseChangeSystemMetaInfo getMetaInfo() {
        return new DummyDcsMetaInfo();
    }

    @NotNull
    @Override
    public List<DatabaseChangeSet> listDatabaseChangeSetsForProject(@NotNull Project project) throws DatacTechnicalException {
        List<DatabaseChangeSet> changeSets = new ArrayList<>();

        switch (CHECKED_OUT_REVISION) {
            case 6:
                changeSets.add(
                        new DatabaseChangeSet()
                                .setInternalId("A")
                                .setSourceFilename("DUMMY")
                                .setChecksum(CHECKSUM_A_ORIGINAL)
                                .addChange(buildDummyChange())
                );
                break;
            case 5:
                changeSets.add(
                        new DatabaseChangeSet()
                                .setInternalId("A")
                                .setSourceFilename("DUMMY")
                                .setChecksum(CHECKSUM_A_MODIFIED_5)
                                .addChange(buildDummyChange())
                );
                break;
            case 4:
                changeSets.add(
                        new DatabaseChangeSet()
                                .setInternalId("A")
                                .setSourceFilename("DUMMY")
                                .setChecksum(CHECKSUM_A_MODIFIED_4)
                                .addChange(buildDummyChange())
                );
                changeSets.add(
                        new DatabaseChangeSet()
                                .setInternalId("B")
                                .setSourceFilename("DUMMY")
                                .setChecksum(CHECKSUM_B_ORIGINAL)
                                .addChange(buildDummyChange())
                );
                break;
            case 3:
                changeSets.add(
                        new DatabaseChangeSet()
                                .setInternalId("A")
                                .setSourceFilename("DUMMY")
                                .setChecksum(CHECKSUM_A_MODIFIED_5)
                                .addChange(buildDummyChange())
                );
                changeSets.add(
                        new DatabaseChangeSet()
                                .setInternalId("C")
                                .setSourceFilename("DUMMY")
                                .setChecksum(CHECKSUM_C_ORIGINAL)
                                .addChange(buildDummyChange())
                );
                break;
            case 2:
                changeSets.add(
                        new DatabaseChangeSet()
                                .setInternalId("A")
                                .setSourceFilename("DUMMY")
                                .setChecksum(CHECKSUM_A_MODIFIED_4)
                                .addChange(buildDummyChange())
                );
                changeSets.add(
                        new DatabaseChangeSet()
                                .setInternalId("B")
                                .setSourceFilename("DUMMY")
                                .setChecksum(CHECKSUM_B_ORIGINAL)
                                .addChange(buildDummyChange())
                );
                changeSets.add(
                        new DatabaseChangeSet()
                                .setInternalId("C")
                                .setSourceFilename("DUMMY")
                                .setChecksum(CHECKSUM_C_ORIGINAL)
                                .addChange(buildDummyChange())
                );
                break;
            case 1:
            case 0: // Change 0 and 1 have the same data
                changeSets.add(
                        new DatabaseChangeSet()
                                .setInternalId("A")
                                .setSourceFilename("DUMMY")
                                .setChecksum(CHECKSUM_A_MODIFIED_4)
                                .addChange(buildDummyChange())
                );
                changeSets.add(
                        new DatabaseChangeSet()
                                .setInternalId("B")
                                .setSourceFilename("DUMMY")
                                .setChecksum(CHECKSUM_B_ORIGINAL)
                                .addChange(buildDummyChange())
                );
                changeSets.add(
                        new DatabaseChangeSet()
                                .setInternalId("C")
                                .setSourceFilename("DUMMY")
                                .setChecksum(CHECKSUM_C_MODIFIED)
                                .addChange(buildDummyChange())
                );
                break;
        }


        return changeSets;
    }

    @Override
    public void prepareDeployment(@NotNull Project project, @NotNull DeploymentInstance targetInstance, @NotNull DatabaseChangeSet changeSet) throws DatacTechnicalException {
        // Nothing to do...
    }

    private DatabaseChange buildDummyChange() {
        return new DatabaseChange()
                .setChecksum(RandomStringUtils.random(16))
                .setType("SQL");
    }
}
