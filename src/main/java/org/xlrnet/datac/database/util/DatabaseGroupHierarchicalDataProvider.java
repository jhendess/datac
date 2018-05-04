package org.xlrnet.datac.database.util;

import com.vaadin.data.provider.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.xlrnet.datac.database.domain.DeploymentGroup;
import org.xlrnet.datac.database.domain.DeploymentRoot;
import org.xlrnet.datac.database.domain.IDatabaseInstance;
import org.xlrnet.datac.database.services.DatabaseDeploymentManagementService;
import org.xlrnet.datac.foundation.domain.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public class DatabaseGroupHierarchicalDataProvider extends AbstractBackEndHierarchicalDataProvider<IDatabaseInstance, Object> {

    /** Service for accessing deployment groups and instances. */
    private final DatabaseDeploymentManagementService deploymentManagementService;

    /** The project root in which all deployment groups must reside. */
    @Getter(AccessLevel.PROTECTED)
    private final Project project;

    /** Virtual root group. */
    private DeploymentRoot deploymentRoot;

    public DatabaseGroupHierarchicalDataProvider(DatabaseDeploymentManagementService deploymentManagementService, Project project) {
        this.deploymentManagementService = deploymentManagementService;
        this.project = project;
        deploymentRoot = new DeploymentRoot("<PROJECT ROOT>");
    }

    @Override
    public int getChildCount(HierarchicalQuery query) {
        return 0;
    }

    @Override
    public boolean hasChildren(IDatabaseInstance item) {
        if (item instanceof DeploymentGroup) {
            DeploymentGroup deploymentGroup = (DeploymentGroup) item;
            return deploymentManagementService.hasChildGroups(deploymentGroup);
        } else if (item instanceof DeploymentRoot) {
            return deploymentManagementService.countGroupsByProject(project);
        }
        throw new IllegalStateException(String.format("Item must be either %s or %s", DeploymentGroup.class, DeploymentRoot.class));
    }

    @Override
    protected Stream<IDatabaseInstance> fetchChildrenFromBackEnd(HierarchicalQuery query) {
        Optional parentOptional = query.getParentOptional();
        List<IDatabaseInstance> result = new ArrayList<>();
        if (parentOptional.isPresent()) {
            if (parentOptional.get() instanceof DeploymentRoot) {
                Set<DeploymentGroup> rootGroups = deploymentManagementService.findRootGroupsByProject(getProject());
                result.addAll(rootGroups);
            } else if (parentOptional.get() instanceof DeploymentGroup) {
                Set<DeploymentGroup> groupsByParent = deploymentManagementService.findDeploymentGroupsByParent((DeploymentGroup) parentOptional.get());
                result.addAll(groupsByParent);
            } else {
                throw new IllegalStateException();
            }
        } else {
            result.add(deploymentRoot);
        }
        return result.stream();
    }
}
