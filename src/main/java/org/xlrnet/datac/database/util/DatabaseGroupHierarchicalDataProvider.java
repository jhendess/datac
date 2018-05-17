package org.xlrnet.datac.database.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.xlrnet.datac.database.domain.DeploymentGroup;
import org.xlrnet.datac.database.domain.DeploymentRoot;
import org.xlrnet.datac.database.domain.IDatabaseInstance;
import org.xlrnet.datac.database.services.DeploymentGroupService;
import org.xlrnet.datac.foundation.domain.Project;

import com.vaadin.data.provider.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DatabaseGroupHierarchicalDataProvider extends AbstractBackEndHierarchicalDataProvider<IDatabaseInstance, Object> {

    /** Service for accessing deployment groups and instances. */
    private final DeploymentGroupService deploymentManagementService;

    /** The project root in which all deployment groups must reside. */
    @Getter(AccessLevel.PROTECTED)
    private final Project project;

    /** Flag whether instances should be included or not. */
    private final boolean includeInstances;

    /** Virtual root group. */
    @Getter
    private DeploymentRoot deploymentRoot;

    public DatabaseGroupHierarchicalDataProvider(DeploymentGroupService deploymentManagementService, Project project, boolean includeInstances) {
        this.deploymentManagementService = deploymentManagementService;
        this.project = project;
        this.includeInstances = includeInstances;
        deploymentRoot = new DeploymentRoot("<PROJECT ROOT>");
    }

    @Override
    public int getChildCount(HierarchicalQuery query) {
        return 0;
    }

    @Override
    public boolean hasChildren(IDatabaseInstance item) {
        boolean hasChildren = false;
        if (item instanceof DeploymentGroup) {
            DeploymentGroup deploymentGroup = (DeploymentGroup) item;
            if (includeInstances && !deploymentGroup.getInstances().isEmpty()) {
                hasChildren = true;
            } else {
                hasChildren = deploymentManagementService.hasChildGroups(deploymentGroup);
            }
        } else if (item instanceof DeploymentRoot) {
            hasChildren = deploymentManagementService.countGroupsByProject(project);
        }
        return hasChildren;
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
                DeploymentGroup parentGroup = (DeploymentGroup) parentOptional.get();
                Set<DeploymentGroup> groupsByParent = deploymentManagementService.findDeploymentGroupsByParent(parentGroup);
                result.addAll(groupsByParent);
                if (includeInstances) {
                    result.addAll(parentGroup.getInstances());
                }
            } else {
                throw new IllegalStateException(String.format("Unexpected parent of type %s", parentOptional.get().getClass()));
            }
        } else {
            result.add(deploymentRoot);
        }
        return result.stream();
    }
}
