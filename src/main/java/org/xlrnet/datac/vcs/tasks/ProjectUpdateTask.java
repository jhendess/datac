package org.xlrnet.datac.vcs.tasks;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.tasks.AbstractRunnableTask;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.FileService;
import org.xlrnet.datac.vcs.api.VcsAdapter;
import org.xlrnet.datac.vcs.api.VcsMetaInfo;
import org.xlrnet.datac.vcs.services.LockingService;
import org.xlrnet.datac.vcs.services.VersionControlSystemService;

import java.util.Optional;

/**
 * The central task which performs a project update.
 */
public class ProjectUpdateTask extends AbstractRunnableTask<Project> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectUpdateTask.class);

    /** The project which should be updated. */
    private final Project project;

    /** The VCS Adapter that will be used for updating the project. */
    private final VersionControlSystemService vcsService;

    /** Central locking service. */
    private final LockingService lockingService;

    /** The file service for accessing file resources. */
    private final FileService fileService;

    public ProjectUpdateTask(Project project, VersionControlSystemService vcsService, LockingService lockingService, FileService fileService) {
        this.project = project;
        this.vcsService = vcsService;
        this.lockingService = lockingService;
        this.fileService = fileService;
    }

    @Override
    public void runTask() {
        if (lockingService.tryLock(project)) {
            try {
                getRunningStatusHandler().handleStatusChange(true);

                LOGGER.info("Begin update of project {} [{}]", project.getId(), project.getName());
                updateProject();
                LOGGER.info("Finished updated project {} [{}] successfully", project.getId(), project.getName());
            } catch (DatacTechnicalException e) {
                LOGGER.error("Update of project {} [{}] failed due to unexpected exception", project.getId(), project.getName());
            } finally {
                lockingService.unlock(project);
            }
        } else {
            LOGGER.warn("Update of project {} [{}] failed because project is locked", project.getId(), project.getName());
        }
    }

    private void updateProject() throws DatacTechnicalException {
        VcsAdapter vcsAdapter = getVcsAdapter();
    }

    /**
     * Try to resolve the correct VCS adapter for this project. If no adapter with the same class could be found, the
     * application tries to fall back to a adapter which implements the same VCS type.
     *
     * @return The correct VCS adapter for the project.
     * @throws DatacTechnicalException Will be thrown if no VCS adapter could be resolved
     */
    @NotNull
    private VcsAdapter getVcsAdapter() throws DatacTechnicalException {
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
