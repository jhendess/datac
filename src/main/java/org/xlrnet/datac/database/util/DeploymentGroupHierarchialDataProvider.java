package org.xlrnet.datac.database.util;

import com.vaadin.data.provider.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.data.provider.HierarchicalQuery;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.xlrnet.datac.database.domain.DeploymentGroup;
import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.database.domain.IDatabaseInstance;
import org.xlrnet.datac.database.services.DatabaseDeploymentManagementService;
import org.xlrnet.datac.foundation.domain.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public class DeploymentGroupHierarchialDataProvider extends AbstractBackEndHierarchicalDataProvider<IDatabaseInstance, Object> {

    /** Service for accessing deployment groups and instances. */
    private final DatabaseDeploymentManagementService deploymentManagementService;

    /** The project root in which all deployment groups must reside. */
    @Getter(AccessLevel.PROTECTED)
    private final Project project;

    public DeploymentGroupHierarchialDataProvider(DatabaseDeploymentManagementService deploymentManagementService, Project project) {
        this.deploymentManagementService = deploymentManagementService;
        this.project = project;
    }

    @Override
    public int getChildCount(HierarchicalQuery query) {
        return 0;
    }

    @Override
    public boolean hasChildren(IDatabaseInstance item) {
        if (item instanceof DeploymentGroup) {
            DeploymentGroup deploymentGroup = (DeploymentGroup) item;
            if (deploymentGroup.getInstances().size() > 0) {
                return true;
            } else {
                return deploymentManagementService.hasChildGroups(deploymentGroup);
            }
        } else if (item instanceof DeploymentInstance) {
            return false;
        }
        return false;
    }

    @Override
    protected Stream<IDatabaseInstance> fetchChildrenFromBackEnd(HierarchicalQuery query) {
        Optional parentOptional = query.getParentOptional();
        List<IDatabaseInstance> result = new ArrayList<>();
        if (parentOptional.isPresent()) {
            DeploymentGroup parentGroup = (DeploymentGroup) parentOptional.get();
            Set<DeploymentGroup> groupsByParent = deploymentManagementService.findDeploymentGroupsByParent(parentGroup);
            result.addAll(groupsByParent);
            result.addAll(parentGroup.getInstances());
        } else {
            Set<DeploymentGroup> rootGroups = deploymentManagementService.findRootGroupsByProject(getProject());
            result.addAll(rootGroups);
        }
        return result.stream();
    }
}
