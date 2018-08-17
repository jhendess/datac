package org.xlrnet.datac.administration.tasks;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.tasks.AbstractRunnableTask;
import org.xlrnet.datac.database.services.ChangeIndexingService;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.ProjectState;
import org.xlrnet.datac.foundation.services.FileService;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.vcs.api.VcsAdapter;
import org.xlrnet.datac.vcs.api.VcsLocalRepository;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.RevisionGraphService;
import org.xlrnet.datac.vcs.services.VersionControlSystemRegistry;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

@Slf4j
public class ChecksumRecalculationTask extends AbstractRunnableTask {

    private final ProjectService projectService;

    private final ChangeIndexingService indexingService;

    private final VersionControlSystemRegistry vcsRegistry;

    private final RevisionGraphService revisionGraphService;

    private final FileService fileService;

    private final Set<ProjectState> INVALID_STATES = Sets.newHashSet(ProjectState.MISSING_LOG, ProjectState.DIRTY, ProjectState.NEW);

    public ChecksumRecalculationTask(ProjectService projectService, ChangeIndexingService indexingService, VersionControlSystemRegistry vcsRegistry, RevisionGraphService revisionGraphService, FileService fileService) {
        super();
        this.projectService = projectService;
        this.indexingService = indexingService;
        this.vcsRegistry = vcsRegistry;
        this.revisionGraphService = revisionGraphService;
        this.fileService = fileService;
    }

    @Override
    protected void runTask() {
        // TODO: Add event logging
        LOGGER.info("Beginning recalculation of all change set checksums");
        Iterable<Project> projects = projectService.findAll();
        for (Project project : projects) {
            if (project.isInitialized() && !INVALID_STATES.contains(project.getState())) {
                recalculateProject(project);
            } else {
                LOGGER.warn("Skipping project {} [id={}] in invalid state {}", project.getName(), project.getId(), project.getState());
            }
        }
        LOGGER.info("Finished recalculation of all change set checksums");
    }

    private void recalculateProject(Project project) {
        LOGGER.info("Processing project {} [id={}]", project.getName(), project.getId());
        project.setState(ProjectState.MAINTENANCE);
        project = projectService.saveAndPublishStateChange(project, 0);
        try {
            Path projectRepositoryPath = fileService.getProjectRepositoryPath(project);
            VcsAdapter vcsAdapter = vcsRegistry.getVcsAdapter(project);
            VcsLocalRepository localRepository = vcsAdapter.openLocalRepository(projectRepositoryPath, project);
            Collection<Revision> changesInProject = revisionGraphService.findAllWithModifyingDatabaseChangesInProject(project);
            int finished = 0;
            for (Revision revision : changesInProject) {
                indexingService.recalculateChecksumsInRevision(project, revision, localRepository);
                double progress = (double) finished++ / changesInProject.size() * 100.f;
                projectService.publishProgressChange(project, progress);
            }
            project.setState(ProjectState.FINISHED);
            project = projectService.saveAndPublishStateChange(project, 0);
        } catch (RuntimeException | DatacTechnicalException e) {
            LOGGER.error("Recalculating checksums in project {} [id={}] failed", e);
            project.setState(ProjectState.ERROR);
            projectService.saveAndPublishStateChange(project, 0);
        }
    }
}
