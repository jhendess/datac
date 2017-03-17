package org.xlrnet.datac.vcs.services;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.FileService;
import org.xlrnet.datac.vcs.api.VcsAdapter;
import org.xlrnet.datac.vcs.api.VcsMetaInfo;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Service which is responsible for collecting all database changes in a project.
 */
@Service
public class ProjectUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectUpdateService.class);

    /** The VCS Adapter that will be used for updating the project. */
    private final VersionControlSystemService vcsService;

    /** Central locking service. */
    private final LockingService lockingService;

    /** The file service for accessing file resources. */
    private final FileService fileService;

    @Autowired
    public ProjectUpdateService(VersionControlSystemService vcsService, LockingService lockingService, FileService fileService) {
        this.vcsService = vcsService;
        this.lockingService = lockingService;
        this.fileService = fileService;
    }

    /**
     * Try to queue a new project update. If the project is currently locked for writing, no update will be queued.
     *
     * @param project
     *         The project that should be updated.
     * @return True if the project update could be queued successfully or false if the project is currently locked for
     * writing.
     */
    public boolean queueProjectUpdate(@NotNull Project project) {
        checkArgument(project.getId() != null, "Project must be persisted");

        if (!lockingService.isLocked(project)) {
            startAsynchronousProjectUpdate(project);
            LOGGER.debug("Successfully queued update task for project {} [{}]", project.getId(), project.getName());
            return true;
        }
        LOGGER.debug("Queuing update task for project {} [{}] failed: project is locked", project.getId(), project.getName());
        return false;
    }

    /**
     * Asynchronously trigger a project change update. 
     * 
     * @param project The project to update.
     */
    @Async
    public void startAsynchronousProjectUpdate(@NotNull Project project) {
        if (lockingService.tryLock(project)) {
            try {
                LOGGER.info("Begin update of project {} [{}]", project.getName(), project.getId());
                updateProject(project);
                LOGGER.info("Finished updated project {} [{}] successfully", project.getName(), project.getId());
            } catch (DatacTechnicalException e) {
                LOGGER.error("Update of project {} [{}] failed due to unexpected exception", project.getName(), project.getId());
            } finally {
                lockingService.unlock(project);
            }
        } else {
            LOGGER.warn("Update of project {} [{}] failed because project is locked", project.getName(), project.getId());
        }
    }

    /**
     * Internal main method for updating the project.
     *
     * @param project
     * @throws DatacTechnicalException
     */
    private void updateProject(@NotNull  Project project) throws DatacTechnicalException {
        VcsAdapter vcsAdapter = getVcsAdapter(project);
    }

    /**
     * Try to resolve the correct VCS adapter for this project. If no adapter with the same class could be found, the
     * application tries to fall back to a adapter which implements the same VCS type.
     *
     * @return The correct VCS adapter for the project.
     * @throws DatacTechnicalException Will be thrown if no VCS adapter could be resolved
     */
    @NotNull
    private VcsAdapter getVcsAdapter(@NotNull Project project) throws DatacTechnicalException {
        Optional<VcsMetaInfo> metaInfo = vcsService.findMetaInfoByAdapterClassName(project.getAdapterClass());
        if (!metaInfo.isPresent()) {
            metaInfo = vcsService.findMetaInfoByVcsType(project.getType());
            metaInfo.ifPresent(m -> LOGGER.warn("No VCS of class {} found - falling back to adapter {} with same type {}", project.getAdapterClass(), m.getAdapterName(), m.getVcsName()));
        }
        if (!metaInfo.isPresent()) {
            throw new DatacTechnicalException("No VCS adapter of type " + project.getType() + " or class " + project.getAdapterClass() + " is available");
        }

        Optional<VcsAdapter> adapterByMetaInfo = vcsService.findAdapterByMetaInfo(metaInfo.get());
        if (adapterByMetaInfo.isPresent()) {
            return adapterByMetaInfo.get();
        } else {
            throw new DatacTechnicalException("Resolving VCS adapter failed");
        }
    }
}
