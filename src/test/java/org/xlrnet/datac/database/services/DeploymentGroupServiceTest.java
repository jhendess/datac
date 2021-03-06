package org.xlrnet.datac.database.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.AbstractSpringBootTest;
import org.xlrnet.datac.database.domain.DatabaseConnection;
import org.xlrnet.datac.database.domain.DeploymentGroup;
import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.test.domain.EntityCreatorUtil;
import org.xlrnet.datac.vcs.domain.Branch;

public class DeploymentGroupServiceTest extends AbstractSpringBootTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private DeploymentGroupService deploymentManagementService;

    @Autowired
    private DatabaseConnectionService connectionService;

    @Autowired
    private DeploymentInstanceService instanceService;

    private Project project;

    @Before
    public void setup() {
        Project entity = EntityCreatorUtil.buildProject();
        Branch branch = EntityCreatorUtil.buildBranch();
        entity.addBranch(branch.setDevelopment(true));
        project = projectService.save(entity);
    }

    @Test
    public void testPersistDatabaseGroup() {
        DeploymentGroup rootGroup = new DeploymentGroup("Test group", project);
        DeploymentGroup childGroup1 = new DeploymentGroup("Child 1", project);
        DeploymentGroup childGroup2 = new DeploymentGroup("Child 2", project);

        DatabaseConnection rootConnection = connectionService.save(EntityCreatorUtil.buildDatabaseConnection("rootConnection"));
        DatabaseConnection childConnection1 = connectionService.save(EntityCreatorUtil.buildDatabaseConnection("childConnection1"));
        DatabaseConnection childConnection21 = connectionService.save(EntityCreatorUtil.buildDatabaseConnection("childConnection21"));
        DatabaseConnection childConnection22 = connectionService.save(EntityCreatorUtil.buildDatabaseConnection("childConnection22"));

        rootGroup.addChildGroup(childGroup1);
        rootGroup.addChildGroup(childGroup2);

        rootGroup.addInstance(new DeploymentInstance("rootInstance", rootConnection));
        childGroup1.addInstance(new DeploymentInstance("childInstance1", childConnection1));
        childGroup2.addInstance(new DeploymentInstance("childInstance21", childConnection21));
        childGroup2.addInstance(new DeploymentInstance("childInstance22", childConnection22));

        DeploymentGroup savedRoot = deploymentManagementService.save(rootGroup);

        DeploymentGroup reloaded = deploymentManagementService.findOne(savedRoot.getId());

        assertNotNull(reloaded);
        assertEquals(1, savedRoot.getInstances().size());
        assertEquals(2, savedRoot.getChildren().size());
    }
}