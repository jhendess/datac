package org.xlrnet.datac.foundation.services;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.foundation.configuration.StartupPhases;

/**
 * Service which cleans the working directory on startup when running in test mode.
 */
@Service
@Profile("test")
public class CleanWorkingDirectoryOnStartup implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanWorkingDirectoryOnStartup.class);

    private final FileService fileService;
    private boolean running;

    @Autowired
    public CleanWorkingDirectoryOnStartup(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void start() {
        LOGGER.warn("Cleaning working directory in TEST mode");
        Path workingDirectoryPath = fileService.getWorkingDirectoryPath();
        try {
            Files.list(workingDirectoryPath)
                    .filter(p -> Files.isDirectory(p))
                    .forEach(p -> {
                        try {
                            fileService.deleteRecursively(p);
                        } catch (DatacTechnicalException e) {
                            throw new DatacRuntimeException(e);
                        }
                    });
            running = true;
        } catch (Exception e) {
            running = false;
            throw new DatacRuntimeException(e);
        }
    }

    @Override
    public void stop() {
        // Nothing to do here
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return StartupPhases.INITIALIZATION;
    }
}
