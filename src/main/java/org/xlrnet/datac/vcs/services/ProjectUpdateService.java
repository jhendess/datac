package org.xlrnet.datac.vcs.services;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.ProjectAlreadyInitializedException;
import org.xlrnet.datac.commons.exception.RevisionLoopException;
import org.xlrnet.datac.commons.exception.VcsRepositoryException;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.FileService;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.foundation.services.ValidationService;
import org.xlrnet.datac.vcs.api.*;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;

import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    /** Project service for updating project data. */
    private final ProjectService projectService;

    /** Service for accessing the revision revision graph. */
    private final RevisionGraphService revisionGraphService;

    /** Bean validation service. */
    private final ValidationService validator;

    @Autowired
    public ProjectUpdateService(VersionControlSystemService vcsService, LockingService lockingService, FileService fileService, ProjectService projectService, RevisionGraphService revisionGraphService, ValidationService validator) {
        this.vcsService = vcsService;
        this.lockingService = lockingService;
        this.fileService = fileService;
        this.projectService = projectService;
        this.revisionGraphService = revisionGraphService;
        this.validator = validator;
    }

    /**
     * Asynchronously trigger a project change update.
     *
     * @param project
     *         The project to update.
     */
    @Async
    public void startAsynchronousProjectUpdate(@NotNull Project project) {
        if (lockingService.tryLock(project)) {
            try {
                LOGGER.info("Begin update of project {}", project.getName());
                Project reloaded = projectService.findOne(project.getId());
                updateProject(reloaded);
                LOGGER.info("Finished updating project {} [id={}] successfully", project.getName(), project.getId());
            } catch (DatacTechnicalException e) {
                LOGGER.error("Update of project {} [id={}] failed because of an unexpected exception", project.getName(), project.getId(), e);
            } finally {
                lockingService.unlock(project);
            }
        } else {
            LOGGER.warn("Update of project {} [id={}] failed because project is locked", project.getName(), project.getId());
        }
    }

    /**
     * Internal main method for updating a project. If the project repository is not yet initialized, the VCS adapter
     * will be called to initialize a local repository.
     *
     * @param project
     *         The project to update.
     * @throws DatacTechnicalException
     *         Will be thrown if the project update failed.
     */
    @Transactional
    protected void updateProject(@NotNull Project project) throws DatacTechnicalException {
        VcsAdapter vcsAdapter = getVcsAdapter(project);

        try {
            if (!project.isInitialized()) {
                initializeProjectRepository(project, vcsAdapter);
            }

            Path repositoryPath = fileService.getProjectRepositoryPath(project);

            LOGGER.debug("Opening local repository at {}", repositoryPath.toString());
            VcsLocalRepository localRepository = vcsAdapter.openLocalRepository(repositoryPath, project);

            Project updatedProject = updateRevisions(project, localRepository);
            indexDatabaseChanges(updatedProject);

            updatedProject.setLastChangeCheck(LocalDateTime.now());
            projectService.save(updatedProject);

        } catch (RuntimeException | IOException e) {
            throw new DatacTechnicalException("Project update failed", e);
        }
    }

    /**
     * Initialize a new project repository. This will first call the file service to create necessary file structures
     * and afterwards open a remote connection to the project's VCS to initialize local VCS files.
     *
     * @param project
     *         The project to update.
     * @param vcsAdapter
     *         The adapter to use for updating the project.
     * @throws DatacTechnicalException
     *         Will be thrown if the project update failed
     * @throws IOException
     *         Will be thrown on writing errors
     */
    protected void initializeProjectRepository(@NotNull Project project, @NotNull VcsAdapter vcsAdapter) throws DatacTechnicalException, IOException {
        LOGGER.info("Initializing new repository for project {}", project.getName());
        VcsRemoteRepositoryConnection vcsRemoteRepositoryConnection = vcsAdapter.connectRemote(project);
        VcsConnectionStatus vcsConnectionStatus = vcsRemoteRepositoryConnection.checkConnection();
        if (vcsConnectionStatus != VcsConnectionStatus.ESTABLISHED) {
            throw new DatacTechnicalException("Connection check failed. Status was " + vcsConnectionStatus);
        }

        Path repositoryPath;
        try {
            repositoryPath = fileService.prepareProjectRepositoryPath(project);
        } catch (ProjectAlreadyInitializedException pe) {   // NOSONAR: No logging necessary, since part of logic flow
            LOGGER.info("Repository for project {} [id={}] was already initialized - cleaning existing repository", project.getName(), project.getId());
            fileService.deleteProjectRepository(project);
            repositoryPath = fileService.prepareProjectRepositoryPath(project);
        }

        try {
            LOGGER.debug("Calling remote VCS adapter {} to initialize repository in {}", vcsRemoteRepositoryConnection.getClass().getName(), repositoryPath);
            vcsRemoteRepositoryConnection.initializeLocalRepository(repositoryPath, project.getDevelopmentBranch());

            project.setInitialized(true);
            projectService.save(project);
        } catch (DatacTechnicalException | RuntimeException e) {
            LOGGER.error("Initialization of project {} [id={}] failed - rolling back file system changes", e);
            try {
                fileService.deleteProjectRepository(project);
            } catch (DatacTechnicalException e2) {
                LOGGER.error("Critical error while cleaning up file system during rollback", e2);
            }
            throw e;
        }

        LOGGER.info("Successfully initialized local repository for project {}", project.getName());
    }

    /**
     * Update the internal revision graph of the VCS. Checks for new branches and updates the revisions.
     *
     * @param project
     *         The project to update.
     * @param localRepository
     *         Local VCS repository instance for the project to edit.
     */
    protected Project updateRevisions(Project project, VcsLocalRepository localRepository) throws VcsConnectionException, VcsRepositoryException, RevisionLoopException {
        LOGGER.debug("Checking for new branches in project {}", project.getName());
        Project updatedProject = projectService.updateAvailableBranches(project, localRepository);

        LOGGER.debug("Updating revisions in project {}", updatedProject.getName());

        for (Branch branch : updatedProject.getBranches()) {
            if (branch.isWatched()) {
                updateRevisionsInBranch(updatedProject, branch, localRepository);
            } else {
                LOGGER.debug("Skipping branch {} in project {}", branch.getName(), updatedProject.getName());
            }
        }
        LOGGER.debug("Finished revision update in project {}", updatedProject.getName());
        return updatedProject;
    }

    protected void updateRevisionsInBranch(@NotNull Project project, @NotNull Branch branch, @NotNull VcsLocalRepository localRepository) throws VcsConnectionException, VcsRepositoryException, RevisionLoopException {
        LOGGER.debug("Updating revisions on branch {} in project {}", branch.getName(), project.getName());
        localRepository.updateRevisionsFromRemote(branch);

        VcsRevision rootRevision = localRepository.fetchLatestRevisionInBranch(branch);

        Revision revision = convertRevision(rootRevision, project);
        LOGGER.trace("Saving revisions on branch {} in project {}", branch.getName(), project.getName());
        revisionGraphService.save(revision);
        LOGGER.debug("Finished updating revisions on branch {} in project {}", branch.getName(), project.getName());
    }

    /**
     * Recursive implementation which converts external {@link VcsRevision} objects to {@link Revision} entities. If
     * any of the revision objects already exist in the database, the rest of the graph will be fetched from the
     * database.
     *
     * @param rootRevision
     *         The root revision used for starting the conversion.
     * @param project
     *         The project in which the revisions will be stored.
     */
    @NotNull
    protected Revision convertRevision(@NotNull VcsRevision rootRevision, @NotNull Project project) throws RevisionLoopException {
        Revision revision = revisionGraphService.findRevisionInProject(project, rootRevision.getInternalId());
        if (revision == null) {
            validator.checkConstraints(rootRevision);
            LOGGER.trace("Creating new revision {}", rootRevision.getInternalId());
            revision = new Revision(rootRevision).setProject(project);
            return convertRevision(rootRevision, revision, project, new HashMap<>());
        } else {
            return revision;
        }
    }

    @NotNull
    private Revision convertRevision(@NotNull VcsRevision revision, @NotNull Revision converted, @NotNull Project project, @NotNull Map<String, Revision> processRevisions) throws RevisionLoopException {
        String internalId = revision.getInternalId();
        if (processRevisions.containsKey(internalId)) {
            LOGGER.debug("Encountered visited revision {}", internalId);
            return processRevisions.get(internalId);
        }
        validator.checkConstraints(revision);
        for (VcsRevision rawParent : revision.getParents()) {
            String rawParentInternalId = rawParent.getInternalId();
            if (revisionGraphService.existsRevisionInProject(project, rawParentInternalId)) {
                // Use the existing revision as a parent
                LOGGER.trace("Found existing revision {} - aborting loop", rawParentInternalId);
                Revision revisionInProject = revisionGraphService.findRevisionInProject(project, rawParentInternalId);
                converted.addParent(revisionInProject);
                processRevisions.put(internalId, revisionInProject);
            } else if (processRevisions.containsKey(rawParentInternalId)) {
                LOGGER.trace("Found processed revision {} - aborting loop", rawParentInternalId);
                converted.addParent(processRevisions.get(rawParentInternalId));
            } else {
                // Create a new parent and continue the loop
                LOGGER.trace("Creating new revision {}", rawParentInternalId);
                Revision parent = new Revision(rawParent).setProject(project);
                converted.addParent(parent);
                processRevisions.put(internalId, parent);
                convertRevision(rawParent, parent, project, processRevisions);
            }
        }
        return converted;
    }

    private void indexDatabaseChanges(@NotNull Project project) {
        LOGGER.info("Begin indexing changes in project {}", project.getName());

        // TODO
    }

    /**
     * Try to resolve the correct VCS adapter for this project. If no adapter with the same class could be found, the
     * application tries to fall back to a adapter which implements the same VCS type.
     *
     * @return The correct VCS adapter for the project.
     * @throws DatacTechnicalException
     *         Will be thrown if no VCS adapter could be resolved
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
