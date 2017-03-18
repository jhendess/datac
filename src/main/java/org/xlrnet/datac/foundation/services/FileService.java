package org.xlrnet.datac.foundation.services;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.ProjectAlreadyInitializedException;
import org.xlrnet.datac.foundation.configuration.StartupPhases;
import org.xlrnet.datac.foundation.domain.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Service for accessing the file system.
 */
@Component
@Scope("singleton")
public class FileService implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    /** The configuration value for the file directory which should be used for storing files. */
    @Value("${datac.fileDirectory}")
    private String fileDirectoryConfiguration;

    /** Path object for the working directory. */
    Path workPath;

    private boolean running = false;

    @Override
    public void start() {
        LOGGER.info("Starting file service...");
        checkDirectory();
    }

    @Override
    public boolean isAutoStartup() {
        // The file system must be initialized on boot
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public int getPhase() {
        return StartupPhases.PREPARATION;
    }

    @Override
    public void stop() {
        // No stop action necessary
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Prepare the folder structure for a new project repository. If the repository already exists, an {@link
     * IllegalStateException} will be thrown.
     *
     * @param project
     *         The project for a repository should be prepared
     * @return The path to the repository.
     */
    @NotNull
    public Path prepareProjectRepositoryPath(@NotNull Project project) throws ProjectAlreadyInitializedException, IOException {
        Path repositoryPath = getProjectRepositoryPath(project);

        if (Files.isDirectory(repositoryPath)) {
            throw new ProjectAlreadyInitializedException("Directory structure " + repositoryPath.toString() + " already exists");
        }

        LOGGER.debug("Preparing project repository at {}", repositoryPath.toString());
        Files.createDirectory(repositoryPath);
        LOGGER.debug("Created new project repository at {}", repositoryPath.toString());

        return repositoryPath;
    }

    /**
     * Deletes a local project repository including all files recursively.
     *
     * @param project
     *         The project whose repository should be deleted.
     * @throws DatacTechnicalException
     *         Will be thrown if the deletion fails.
     */
    public void deleteProjectRepository(@NotNull Project project) throws DatacTechnicalException {
        Path projectRepositoryPath = getProjectRepositoryPath(project);
        LOGGER.info("Deleting directory {} recursively", projectRepositoryPath.toString());
        try {
            if (Files.exists(projectRepositoryPath)) {
                Files.walk(projectRepositoryPath, FileVisitOption.FOLLOW_LINKS)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .peek((f) -> LOGGER.trace("Deleting {}", f))
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            LOGGER.error("Deleting directory {} failed - the file system may be in an inconsistent state.", projectRepositoryPath.toString(), e);
            throw new DatacTechnicalException(e);
        }
        LOGGER.info("Deleted directory {} successfully", projectRepositoryPath.toString());
    }

    /**
     * Returns a {@link Path} object which points to the project repository for a given project.
     *
     * @param project
     *         The project for which the path should be returned.
     * @return A path which points to the project repository for a given project.
     */
    @NotNull
    public Path getProjectRepositoryPath(@NotNull Project project) {
        String id = StringUtils.deleteWhitespace(String.valueOf(project.getId()));
        checkArgument(!StringUtils.isBlank(id));
        return workPath.resolve(id);
    }

    private void checkDirectory() {
        workPath = Paths.get(fileDirectoryConfiguration).toAbsolutePath();
        if (Files.isRegularFile(workPath)) {
            throw new DatacRuntimeException("Working path " + workPath.toString() + " is a file");
        } else if (!Files.isDirectory(workPath)) {
            try {
                Files.createDirectory(workPath);
            } catch (IOException e) {
                LOGGER.error("Creating directory " + workPath.toString() + " failed", e);
                throw new DatacRuntimeException("Creating directory " + workPath.toString() + " failed", e);
            }
        }
        LOGGER.info("Using path {} as working directory", workPath.toAbsolutePath().toString());
        running = true;
    }
}
