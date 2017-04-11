package org.xlrnet.datac.foundation.services;

import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.foundation.components.EventLogProxy;
import org.xlrnet.datac.foundation.domain.EventLogMessage;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.repository.ProjectRepository;
import org.xlrnet.datac.vcs.api.VcsConnectionException;
import org.xlrnet.datac.vcs.api.VcsLocalRepository;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;
import org.xlrnet.datac.vcs.domain.Branch;

/**
 * Transactional service for accessing project data.
 */
@Service
public class ProjectService extends AbstractTransactionalService<Project, ProjectRepository> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectService.class);

    /** The file service. */
    private final FileService fileService;

    /** The request-scoped event log (if any). */
    private final EventLogProxy eventLog;

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *  @param crudRepository
     *         The crud repository for providing basic crud operations.
     * @param fileService
     *         Service used for accessing files.
     * @param eventLog
     *         The proxied event log used for logging events.
     */
    @Autowired
    public ProjectService(ProjectRepository crudRepository, FileService fileService, EventLogProxy eventLog) {
        super(crudRepository);
        this.fileService = fileService;
        this.eventLog = eventLog;
    }

    @Transactional
    public Project updateAvailableBranches(@NotNull Project project, @NotNull VcsLocalRepository localRepository) throws VcsConnectionException {
        Pattern pattern = Pattern.compile(project.getNewBranchPattern());
        VcsRemoteRepositoryConnection remoteRepositoryConnection = localRepository.connectToRemote();
        Collection<Branch> remoteBranches = remoteRepositoryConnection.listBranches();
        for (Branch remoteBranch : remoteBranches) {
            // Check if this remote branch doesn't exist yet
            if (project.getBranches().stream()
                    .noneMatch((branch -> StringUtils.equals(branch.getInternalId(), remoteBranch.getInternalId())))) {
                if (pattern.matcher(remoteBranch.getName()).matches()) {
                    LOGGER.debug("Found new matching branch {} [id={}]", remoteBranch.getName(), remoteBranch.getInternalId());
                    remoteBranch.setWatched(true);
                    eventLog.addMessage(new EventLogMessage("Found new matching branch " + remoteBranch.getName()));
                } else {
                    LOGGER.trace("Branch {} [id={}] is new but doesn't match pattern {}", remoteBranch.getName(), remoteBranch.getInternalId(), pattern.toString());
                    eventLog.addMessage(new EventLogMessage("Found new non-matching branch " + remoteBranch.getName()));
                }
                project.addBranch(remoteBranch);
            } else {
                LOGGER.trace("Branch {} [id={}] is not new", remoteBranch.getName(), remoteBranch.getInternalId());
            }
        }
        return save(project);
    }

    /**
     * Performs a clean delete of a project which removes both the entity from the database and working directories from
     * the filesystem.
     *
     * @param entity
     *         The project to delete.
     * @throws DatacTechnicalException
     *         Will be thrown in case of an error while deleting the directory structure.
     */
    @Transactional
    public void deleteClean(Project entity) throws DatacTechnicalException {
        fileService.deleteProjectRepository(entity);
        super.delete(entity);
    }
}
