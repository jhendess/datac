package org.xlrnet.datac.vcs.services;

import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.exception.LiquibaseException;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.sdk.database.MockDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.FileService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Service which provides access to liquibase change log files.
 */
@Service
public class LiquibaseProcessService {

    private final FileService fileService;

    private static final Logger LOGGER = LoggerFactory.getLogger(LiquibaseProcessService.class);

    @Autowired
    public LiquibaseProcessService(FileService fileService) {
        this.fileService = fileService;
    }

    public List<Path> listIncludedPaths(String changeLogFile, Project project) throws DatacTechnicalException {
        try {
            LOGGER.warn("listIncludedPaths() is currently not implemented");
            DatabaseChangeLog databaseChangeLog = getDatabaseChangeLog(changeLogFile, project);

            // TODO: It is currently not possible to detect includes, as they are implicitly hidden in DatabaseChangeLog - see the handleChildNode() method which performs includes automatically

            return new ArrayList<>();
        } catch (LiquibaseException e) {
            LOGGER.error("An unexpected error occurred while listing included paths in changelog {}", changeLogFile, e);
            throw new DatacTechnicalException(e);
        }
    }

    private DatabaseChangeLog getDatabaseChangeLog(String changeLogFile, Project project) throws LiquibaseException {
        ResourceAccessor resourceAccessor = getFileSystemResourceAccessorForProject(project);
        ChangeLogParser parser = ChangeLogParserFactory.getInstance().getParser(changeLogFile, resourceAccessor);
        Database database = getReadOnlyDatabase();
        ChangeLogParameters changeLogParameters = new ChangeLogParameters(database);
        return parser.parse(changeLogFile, changeLogParameters, resourceAccessor);
    }

    private Database getReadOnlyDatabase() {
        return new MockDatabase();
    }

    /**
     * Returns a resource accessor with project-based base path.
     */
    private ResourceAccessor getFileSystemResourceAccessorForProject(Project project) {
        return new FileSystemResourceAccessor(fileService.getProjectRepositoryPath(project).toString());
    }
}
