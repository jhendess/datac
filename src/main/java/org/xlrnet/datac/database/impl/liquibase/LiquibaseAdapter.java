package org.xlrnet.datac.database.impl.liquibase;

import com.google.common.base.Throwables;
import liquibase.change.Change;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.core.H2Database;
import liquibase.exception.LiquibaseException;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ResourceAccessor;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.database.api.DatabaseChangeSystemAdapter;
import org.xlrnet.datac.database.api.DatabaseChangeSystemMetaInfo;
import org.xlrnet.datac.database.domain.DatabaseChange;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.FileService;

import java.util.ArrayList;
import java.util.List;

/**
 * Service which provides access to liquibase change log files.
 */
@Component
public class LiquibaseAdapter implements DatabaseChangeSystemAdapter {

    private static final String UNKNOWN_AUTHOR = "unknown";

    /**
     * Service for accessing the file system.
     */
    private final FileService fileService;

    private static final Logger LOGGER = LoggerFactory.getLogger(LiquibaseAdapter.class);

    @Autowired
    public LiquibaseAdapter(FileService fileService) {
        this.fileService = fileService;
    }

    private DatabaseChangeLog getDatabaseChangeLog(String changeLogFile, Project project) throws LiquibaseException {
        ResourceAccessor resourceAccessor = getFileSystemResourceAccessorForProject(project);
        ChangeLogParser parser = ChangeLogParserFactory.getInstance().getParser(changeLogFile, resourceAccessor);
        Database database = getReadOnlyDatabase();
        ChangeLogParameters changeLogParameters = new ChangeLogParameters(database);
        return parser.parse(changeLogFile, changeLogParameters, resourceAccessor);
    }

    @NotNull
    @Override
    public DatabaseChangeSystemMetaInfo getMetaInfo() {
        return LiquibaseAdapterMetaInfo.getInstance();
    }

    @Override
    @NotNull
    public List<DatabaseChangeSet> listDatabaseChangeSetsForProject(@NotNull Project project) throws DatacTechnicalException {
        ArrayList<DatabaseChangeSet> datacChangeSets = new ArrayList<>();
        LOGGER.debug("Listing database changes in project {}", project);
        try {
            DatabaseChangeLog databaseChangeLog = getDatabaseChangeLog(project.getChangelogLocation(), project);

            int sort = 0;
            for (ChangeSet changeSet : databaseChangeLog.getChangeSets()) {
                DatabaseChangeSet convertedChangeSet = convertChangeSet(changeSet);
                convertedChangeSet.setSort(sort);
                datacChangeSets.add(convertedChangeSet);
                sort++;
            }

        } catch (LiquibaseException e) {
            throw new DatacTechnicalException(e);
        }
        return datacChangeSets;
    }


    /**
     * Convert a given {@link ChangeSet} from liquibase to a {@link DatabaseChangeSet} entity from datac.
     *
     * @param liquibaseChangeSet
     *         The raw liquibase change set.
     * @return A converted change set for datac.
     */
    @NotNull
    private DatabaseChangeSet convertChangeSet(@NotNull ChangeSet liquibaseChangeSet) {
        DatabaseChangeSet datacChangeSet = new DatabaseChangeSet();

        String author = StringUtils.isNotBlank(liquibaseChangeSet.getAuthor()) ? liquibaseChangeSet.getAuthor() : UNKNOWN_AUTHOR;
        datacChangeSet.setAuthor(author)
                .setComment(liquibaseChangeSet.getComments())
                .setInternalId(liquibaseChangeSet.getId())
                .setChecksum(liquibaseChangeSet.generateCheckSum().toString())
                .setSourceFilename(liquibaseChangeSet.getFilePath());

        int changeOrder = 0;
        for (Change change : liquibaseChangeSet.getChanges()) {
            DatabaseChange datacChange = convertChange(change);
            datacChange.setSort(changeOrder);
            datacChangeSet.addChange(datacChange);
            changeOrder++;
        }

        return datacChangeSet;
    }

    /**
     * Convert a given {@link ChangeSet} from liquibase to a {@link DatabaseChange} entity from datac.
     *
     * @param liquibaseChange
     *         The raw liquibase change.
     * @return A converted change set for datac.
     */
    @NotNull
    private DatabaseChange convertChange(@NotNull Change liquibaseChange) {
        Database mockDatabase = getReadOnlyDatabase();
        DatabaseChange datacChange = new DatabaseChange();

        datacChange.setType(liquibaseChange.createChangeMetaData().getName())
                .setDescription(liquibaseChange.getDescription())
                .setChecksum(liquibaseChange.generateCheckSum().toString());

        if (!liquibaseChange.generateStatementsVolatile(mockDatabase)) {
            try {
                StringBuilder sqlBuilder = generateSql(liquibaseChange, mockDatabase);
                datacChange.setPreviewSql(sqlBuilder.toString());
            } catch (DatacTechnicalException e) {
                LOGGER.warn("Generating preview SQL failed: {}", Throwables.getRootCause(e).getMessage());
            }
        } else {
            LOGGER.warn("Couldn't generate preview for change {} - statements are volatile", liquibaseChange.getDescription());
        }


        return datacChange;
    }

    @NotNull
    private StringBuilder generateSql(@NotNull Change change, @NotNull Database database) throws DatacTechnicalException {
        StringBuilder sqlBuilder = new StringBuilder();
        try {
            SqlStatement[] sqlStatements = change.generateStatements(database);
            for (SqlStatement sqlStatement : sqlStatements) {
                Sql[] sqls = SqlGeneratorFactory.getInstance().generateSql(sqlStatement, database);
                for (Sql sql : sqls) {
                    sqlBuilder.append(sql.toSql());
                }
            }
        } catch (RuntimeException e) {
            throw new DatacTechnicalException("Generating preview SQL failed", e);
        }
        return sqlBuilder;
    }

    @NotNull
    private Database getReadOnlyDatabase() {
        return new H2Database();
    }

    /**
     * Returns a resource accessor with project-based base path.
     */
    @NotNull
    private ResourceAccessor getFileSystemResourceAccessorForProject(@NotNull Project project) {
        return new CustomLiquibaseFileSystemResourceAccessor(fileService.getProjectRepositoryPath(project).toString());
    }
}
