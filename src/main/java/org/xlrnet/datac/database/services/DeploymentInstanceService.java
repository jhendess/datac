package org.xlrnet.datac.database.services;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.database.domain.repository.DeploymentInstanceRepository;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

/**
 * Service for accessing and modifying {@link DeploymentInstance} objects.
 */
@Service
public class DeploymentInstanceService extends AbstractTransactionalService<DeploymentInstance, DeploymentInstanceRepository> {

    /** Service for accessing the revision graph. */
    private final RevisionGraphService revisionGraphService;

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *
     * @param crudRepository The crud repository for providing basic crud operations.
     * @param revisionGraphService
     */
    @Autowired
    public DeploymentInstanceService(DeploymentInstanceRepository crudRepository, RevisionGraphService revisionGraphService) {
        super(crudRepository);
        this.revisionGraphService = revisionGraphService;
    }

    @Override
    @Transactional
    public void delete(@NotNull DeploymentInstance entity) {
        entity.getGroup().getInstances().remove(entity);
        super.delete(entity);
    }

    /**
     * Lists all {@link DeploymentInstance} in the given project.
     *
     * @param project The project in which the instance must be.
     * @return Set of deployment instances in the given project or an empty set.
     */
    @NotNull
    @Transactional(readOnly = true)
    public Set<DeploymentInstance> findAllInProject(@NotNull Project project) {
        return getRepository().findAllByProject(project);
    }

    @NotNull
    @Transactional(readOnly = true)
    public Set<DeploymentInstance> findInstancesWithTrackingRevision(Revision revision) {
        Set<DeploymentInstance> allInProject = findAllInProject(revision.getProject());
        Set<DeploymentInstance> filtered = new HashSet<>();
        for (DeploymentInstance deploymentInstance : allInProject) {
            Branch actualBranch = deploymentInstance.getActualBranch();
            boolean revisionOnBranch = revisionGraphService.isRevisionOnBranch(revision, actualBranch);
            if (revisionOnBranch) {
                filtered.add(deploymentInstance);
            }
        }

        return filtered;
    }
}
