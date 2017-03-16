package org.xlrnet.datac.foundation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.foundation.configuration.StartupPhases;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    private Path workPath;

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
