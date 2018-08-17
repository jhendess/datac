package org.xlrnet.datac.database.impl.liquibase;

import ch.qos.logback.classic.Level;
import com.google.common.base.Throwables;
import liquibase.change.Change;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.core.H2Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.LiquibaseParseException;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ResourceAccessor;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.database.api.DatabaseChangeSystemAdapter;
import org.xlrnet.datac.database.api.DatabaseChangeSystemMetaInfo;
import org.xlrnet.datac.database.api.IPreparedDeploymentContainer;
import org.xlrnet.datac.database.domain.DatabaseChange;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.FileService;
import org.xlrnet.datac.vcs.api.VcsAdapter;
import org.xlrnet.datac.vcs.api.VcsLocalRepository;
import org.xlrnet.datac.vcs.services.VersionControlSystemRegistry;

import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service which provides access to liquibase change log files.
 */
@Slf4j
@Component
public class LiquibaseAdapter implements DatabaseChangeSystemAdapter {

    private static final String UNKNOWN_AUTHOR = "unknown";

    private static final String DOES_NOT_EXIST = "does not exist";

    /**
     * Service for accessing the file system.
     */
    private final FileService fileService;

    /**
     * VCS registry.
     */
    private final VersionControlSystemRegistry versionControlSystemRegistry;

    /** Factory for liquibase connections. */
    private final LiquibaseConnectionFactory liquibaseConnectionFactory;

    @Autowired
    public LiquibaseAdapter(FileService fileService, VersionControlSystemRegistry versionControlSystemRegistry, LiquibaseConnectionFactory liquibaseConnectionFactory) {
        this.fileService = fileService;
        this.versionControlSystemRegistry = versionControlSystemRegistry;
        this.liquibaseConnectionFactory = liquibaseConnectionFactory;
    }

    private DatabaseChangeLog getDatabaseChangeLog(String changeLogFile, Project project) throws LiquibaseException {
        LOGGER.debug("Opening Liquibase changelog file at {}", changeLogFile);
        ResourceAccessor resourceAccessor = getFileSystemResourceAccessorForProject(project);
        ChangeLogParser parser = ChangeLogParserFactory.getInstance().getParser(changeLogFile, resourceAccessor);
        Database database = getReadOnlyDatabase();
        ChangeLogParameters changeLogParameters = new ChangeLogParameters(database);
        DatabaseChangeLog parse = parser.parse(changeLogFile, changeLogParameters, resourceAccessor);
        LOGGER.debug("Successfully opened Liquibase changelog file at {}", changeLogFile);
        return parse;
    }

    @PostConstruct
    public void initialize() {
        // TODO: Better use a filter to get rid of the "No database connection available ..." messages?
        LOGGER.info("Disabling liquibase WARN logging level");
        // Disable liquibase warning logging manually (at the moment - this doesn't seem very good)
        ch.qos.logback.classic.Logger liquibaseLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("liquibase");
        liquibaseLogger.setLevel(Level.ERROR);
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
        LOGGER.debug("Listing database changes in project {} [id={}]", project.getName(), project.getId());
        try {
            DatabaseChangeLog databaseChangeLog = getDatabaseChangeLog(project.getChangelogLocation(), project);

            int sort = 0;
            for (ChangeSet changeSet : databaseChangeLog.getChangeSets()) {
                DatabaseChangeSet convertedChangeSet = convertChangeSet(changeSet);
                convertedChangeSet.setSort(sort);
                datacChangeSets.add(convertedChangeSet);
                sort++;
            }

        } catch (LiquibaseParseException pe) {
            if (StringUtils.endsWith(pe.getMessage(), DOES_NOT_EXIST)) {
                LOGGER.warn("Unable to find find changelog file {}", StringUtils.substringBefore(DOES_NOT_EXIST, pe.getMessage()));
            } else {
                throw new DatacTechnicalException(pe);
            }
        } catch (LiquibaseException e) {
            throw new DatacTechnicalException(e);
        }
        return datacChangeSets;
    }

    @NotNull
    @Override
    public IPreparedDeploymentContainer prepareDeployment(@NotNull Project project, @NotNull DeploymentInstance targetInstance, @NotNull DatabaseChangeSet changeSet) throws DatacTechnicalException {
        VcsAdapter vcsAdapter = versionControlSystemRegistry.getVcsAdapter(project);
        VcsLocalRepository vcsLocalRepository = vcsAdapter.openLocalRepository(fileService.getProjectRepositoryPath(project), project);
        vcsLocalRepository.checkoutRevision(changeSet.getRevision());
        LiquibaseDeploymentContainer preparedDeploymentContainer = new LiquibaseDeploymentContainer();

        try {
            DatabaseChangeLog databaseChangeLog = getDatabaseChangeLog(project.getChangelogLocation(), project);
            ChangeSet originalChangeSet = databaseChangeLog.getChangeSet(changeSet.getSourceFilename(), changeSet.getAuthor(), changeSet.getInternalId());
            if (originalChangeSet == null) {
                LOGGER.error("Unable to find change set {} {} {}", changeSet.getSourceFilename(), changeSet.getAuthor(), changeSet.getInternalId());
                throw new DatacTechnicalException("Unable to find change set");
            }

            Database targetDatabase = null;
            try {
                targetDatabase = getDatabaseFromDeploymentInstance(targetInstance);
                List<String> allSql = new ArrayList<>();
                for (Change change : originalChangeSet.getChanges()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    generateSql(change, targetDatabase, stringBuilder);
                    allSql.add(stringBuilder.toString());
                }
                preparedDeploymentContainer.setGeneratedSql(allSql);
            } finally {
                if (targetDatabase != null && targetDatabase.getConnection() != null) {
                    targetDatabase.getConnection().close();
                }
            }
        } catch (LiquibaseParseException pe) {
            if (StringUtils.endsWith(pe.getMessage(), DOES_NOT_EXIST)) {
                LOGGER.warn("Unable to find find changelog file {}", StringUtils.substringBefore(DOES_NOT_EXIST, pe.getMessage()));
            } else {
                throw new DatacTechnicalException(pe);
            }
        } catch (LiquibaseException | SQLException e) {
            LOGGER.error("Unexpected exception occurred while trying to prepare a deployment", e);
            throw new DatacTechnicalException(e);
        }
        return preparedDeploymentContainer;
    }

    private Database getDatabaseFromDeploymentInstance(DeploymentInstance targetInstance) throws DatabaseException, SQLException {
        DatabaseConnection liquibaseConnection = liquibaseConnectionFactory.createDatabaseConnectionFromConfig(targetInstance);
        return DatabaseFactory.getInstance().findCorrectDatabaseImplementation(liquibaseConnection);
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
                StringBuilder stringBuilder = new StringBuilder();
                generateSql(liquibaseChange, mockDatabase, stringBuilder);
                datacChange.setPreviewSql(stringBuilder.toString());
            } catch (DatacTechnicalException e) {
                LOGGER.warn("Generating preview SQL failed: {}", Throwables.getRootCause(e).getMessage());
            }
        } else {
            LOGGER.warn("Couldn't generate preview for change {} - statements are volatile", liquibaseChange.getDescription());
        }


        return datacChange;
    }

    private void generateSql(@NotNull Change change, @NotNull Database database, StringBuilder sqlBuilder) throws DatacTechnicalException {
        try {
            SqlStatement[] sqlStatements = change.generateStatements(database);
            for (SqlStatement sqlStatement : sqlStatements) {
                Sql[] sqls = SqlGeneratorFactory.getInstance().generateSql(sqlStatement, database);
                for (Sql sql : sqls) {
                    sqlBuilder.append(sql.toSql());
                }
            }
        } catch (RuntimeException e) {
            throw new DatacTechnicalException("Generating SQL failed", e);
        }
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
