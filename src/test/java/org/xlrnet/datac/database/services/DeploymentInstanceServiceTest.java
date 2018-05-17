package org.xlrnet.datac.database.services;

import static org.junit.Assert.assertEquals;

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

public class DeploymentInstanceServiceTest extends AbstractSpringBootTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private DeploymentInstanceService instanceService;

    @Autowired
    private DeploymentGroupService groupService;

    @Autowired
    private DatabaseConnectionService connectionService;

    private Project project;

    @Before
    public void setup() {
        Project entity = EntityCreatorUtil.buildProject();
        Branch branch = EntityCreatorUtil.buildBranch();
        entity.addBranch(branch.setDevelopment(true));
        project = projectService.save(entity);
    }

    @Test
    public void testDeleteInstance() {
        DatabaseConnection rootConnection = connectionService.save(EntityCreatorUtil.buildDatabaseConnection("rootConnection"));
        DeploymentGroup rootGroup = groupService.save(new DeploymentGroup("Test group", project));

        DeploymentInstance testInstance = new DeploymentInstance("rootInstance", rootConnection, rootGroup);
        testInstance = instanceService.save(testInstance);

        DeploymentGroup reloadedRoot = groupService.findOne(rootGroup.getId());
        assertEquals(1, reloadedRoot.getInstances().size());

        instanceService.delete(testInstance);       // Should now be deleted

        reloadedRoot = groupService.findOne(rootGroup.getId());
        assertEquals("Instances of reloaded root should be empty after instance deletion", 0, reloadedRoot.getInstances().size());
    }
}