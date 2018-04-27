package org.xlrnet.datac.foundation.services;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.lifecycle.AbstractLifecycleComponent;
import org.xlrnet.datac.foundation.configuration.StartupPhases;

/**
 * Service which cleans the working directory on startup when running in test mode.
 */
@Service
@Profile("test")
public class CleanWorkingDirectoryOnStartup extends AbstractLifecycleComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanWorkingDirectoryOnStartup.class);

    /** Service for accessing files. */
    private final FileService fileService;

    @Autowired
    public CleanWorkingDirectoryOnStartup(FileService fileService) {
        super();
        this.fileService = fileService;
    }

    @Override
    protected void onStart() {
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
        } catch (Exception e) {
            throw new DatacRuntimeException(e);
        }
    }

    @Override
    public int getPhase() {
        return StartupPhases.INITIALIZATION;
    }
}
