package org.xlrnet.datac.database.services;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

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

    private Branch branch;

    @Before
    public void setup() {
        Project entity = EntityCreatorUtil.buildProject();
        branch = EntityCreatorUtil.buildBranch();
        entity.addBranch(branch.setDevelopment(true));
        project = projectService.save(entity);
    }

    @Test
    public void testDeleteInstance() {
        DatabaseConnection rootConnection = connectionService.save(EntityCreatorUtil.buildDatabaseConnection("rootConnection"));
        DeploymentGroup rootGroup = groupService.save(new DeploymentGroup("Test group", project));

        DeploymentInstance testInstance = new DeploymentInstance("rootInstance", rootConnection, rootGroup);
        testInstance.setBranch(branch);
        testInstance = instanceService.save(testInstance);

        DeploymentGroup reloadedRoot = groupService.findOne(rootGroup.getId());
        assertEquals(1, reloadedRoot.getInstances().size());

        instanceService.delete(testInstance);       // Should now be deleted

        reloadedRoot = groupService.findOne(rootGroup.getId());
        assertEquals("Instances of reloaded root should be empty after instance deletion", 0, reloadedRoot.getInstances().size());
    }

    @Test
    public void testBranchInheritance() {
        DatabaseConnection rootConnection = connectionService.save(EntityCreatorUtil.buildDatabaseConnection("rootConnection"));
        DeploymentGroup rootGroup = groupService.save(new DeploymentGroup("Test group", project, branch));

        DeploymentInstance testInstance = new DeploymentInstance("rootInstance", rootConnection, rootGroup);
        testInstance = instanceService.save(testInstance);

        Branch inheritedBranch = testInstance.getActualBranch();
        assertNotNull("Branch should have been inherited, but was null", inheritedBranch);
        assertEquals(branch, inheritedBranch);
    }

    @Test
    public void testBranchInheritance_notOverwritten() {
        final Branch branch2 = EntityCreatorUtil.buildBranch();
        project.addBranch(branch2);
        project = projectService.save(project);
        Branch savedbranch2 = project.getBranches().stream().filter((b) -> b.getName().equalsIgnoreCase(branch2.getName())).findFirst().get();

        DatabaseConnection rootConnection = connectionService.save(EntityCreatorUtil.buildDatabaseConnection("rootConnection"));
        DeploymentGroup rootGroup = groupService.save(new DeploymentGroup("Test group", project, branch));

        DeploymentInstance testInstance = new DeploymentInstance("rootInstance", rootConnection, rootGroup);
        testInstance.setBranch(savedbranch2);
        testInstance = instanceService.save(testInstance);

        Branch inheritedBranch = testInstance.getActualBranch();
        assertNotNull(inheritedBranch);
        assertNotEquals("Branch should not have been inherited, but was", branch, inheritedBranch);
        assertEquals(savedbranch2, inheritedBranch);
    }
}