package org.xlrnet.datac.foundation.services;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.ProjectAlreadyInitializedException;
import org.xlrnet.datac.commons.lifecycle.AbstractLifecycleComponent;
import org.xlrnet.datac.foundation.configuration.StartupPhases;
import org.xlrnet.datac.foundation.domain.Project;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for accessing the file system.
 */
@Slf4j
@Component
@Scope("singleton")
public class FileService extends AbstractLifecycleComponent {

    /**
     * The configuration value for the file directory which should be used for storing files.
     */
    @Value("${datac.fileDirectory}")
    private String fileDirectoryConfiguration;

    /**
     * Path object for the working directory.
     */
    @Getter
    private Path workingDirectoryPath;

    @Autowired
    public FileService() {
        super();
    }

    @Override
    protected void onStart() {
        LOGGER.info("Starting file service");
        initializeWorkingDirectory();
    }

    @Override
    public int getPhase() {
        return StartupPhases.INITIALIZATION;
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
        deleteRecursively(projectRepositoryPath);
    }

    /**
     * Deletes a given file from the file system.
     *
     * @param path
     *         The path to delete.
     * @throws IOException
     *         Will be thrown if deleting fails.
     */
    public void deleteFile(@NotNull Path path) throws IOException {
        LOGGER.debug("Deleting {}", path);
        Files.delete(path);
    }

    /**
     * Returns an absolute {@link Path} object which points to the project repository for a given project.
     *
     * @param project
     *         The project for which the path should be returned.
     * @return A path which points to the project repository for a given project.
     */
    @NotNull
    public Path getProjectRepositoryPath(@NotNull Project project) {
        String id = StringUtils.deleteWhitespace(String.valueOf(project.getId()));
        checkArgument(!StringUtils.isBlank(id));
        return getWorkingDirectoryPath().resolve(id);
    }

    void deleteRecursively(Path pathToDelete) throws DatacTechnicalException {
        LOGGER.info("Deleting directory {} recursively", pathToDelete.toString());
        try {
            if (Files.exists(pathToDelete)) {
                Files.walk(pathToDelete, FileVisitOption.FOLLOW_LINKS)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .peek((f) -> LOGGER.trace("Deleting {}", f))
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            LOGGER.error("Deleting directory {} failed - the file system may be in an inconsistent state.", pathToDelete.toString(), e);
            throw new DatacTechnicalException(e);
        }
        LOGGER.info("Deleted directory {} successfully", pathToDelete.toString());
    }

    private void initializeWorkingDirectory() {
        this.workingDirectoryPath = Paths.get(fileDirectoryConfiguration).toAbsolutePath();
        if (Files.isRegularFile(getWorkingDirectoryPath())) {
            throw new DatacRuntimeException("Working path " + getWorkingDirectoryPath().toString() + " is a file");
        } else if (!Files.isDirectory(getWorkingDirectoryPath())) {
            try {
                Files.createDirectory(getWorkingDirectoryPath());
            } catch (IOException e) {
                LOGGER.error("Creating directory " + getWorkingDirectoryPath().toString() + " failed", e);
                throw new DatacRuntimeException("Creating directory " + getWorkingDirectoryPath().toString() + " failed", e);
            }
        }
        LOGGER.info("Using path {} as working directory", getWorkingDirectoryPath().toAbsolutePath().toString());
    }
}
