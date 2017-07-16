package org.xlrnet.datac.vcs.services;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.AbstractSpringBootTest;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.impl.dummy.DummyDcsAdapter;
import org.xlrnet.datac.database.services.ChangeSetService;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.ProjectState;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.impl.dummy.DummyVcsAdapter;
import org.xlrnet.datac.vcs.impl.dummy.DummyVcsMetaInfo;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration test of a complete project update using dummy adapters.
 */
public class ProjectUpdateServiceFullTest extends AbstractSpringBootTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectUpdateService projectUpdateService;

    @Autowired
    private RevisionGraphService revisionGraphService;

    @Autowired
    private ChangeSetService changeSetService;

    /** The project used for testing. */
    private Project project;

    @Before
    public void setup() {
        project = new Project();
        project.setName("Test project");
        project.setUrl("dummy");
        project.setState(ProjectState.NEW);
        project.setNewBranchPattern(".*");
        project.setChangelogLocation("DUMMY.txt");
        project.setPollInterval(300);
        project.addBranch(new Branch().setInternalId("master").setName("master").setDevelopment(true));
        project.setChangeSystemAdapterClass(DummyDcsAdapter.class.getName());
        project.setVcsAdapterClass(DummyVcsAdapter.class.getName());
        project.setVcsType(DummyVcsMetaInfo.VCS_NAME);
        project = projectService.saveProject(project);
    }

    /** Runs a full project update. See testcase.graphml for an overview of how the expected graph should look like. */
    @Test
    public void testProjectFullUpdate() throws Exception {
        projectUpdateService.startProjectUpdate(project);

        beginTransaction();
        Project reloaded = projectService.findOne(project.getId());
        assertEquals("Project is in unexpected state", ProjectState.FINISHED, reloaded.getState());

        // Verify revision graph
        Revision rootRevision = revisionGraphService.findProjectRootRevision(project);
        assertEquals("6", rootRevision.getInternalId());
        assertEquals(2, rootRevision.getChildren().size());

        for (Revision revision : rootRevision.getChildren()) {
            if ("5".equals(revision.getInternalId())) {
                // Verify revision 5
                assertEquals(1, revision.getChildren().size());
                assertEquals(1, revision.getParents().size());
                assertEquals("6", revision.getParents().get(0).getInternalId());
                Revision rev3 = revision.getChildren().get(0);
                // Verify revision 3
                assertEquals("3", rev3.getInternalId());
                assertEquals(1, rev3.getChildren().size());
                Revision rev2 = rev3.getChildren().get(0);
                // Verify revision 2
                assertEquals("2", rev2.getInternalId());
                assertEquals(2, rev2.getParents().size());
                assertEquals(2, rev2.getChildren().size());
                for (Revision revision10 : rev2.getChildren()) {
                    if (!("0".equals(revision10.getInternalId()) || "1".equals(revision10.getInternalId()))) {
                        fail("Encountered unexpected revision with internal id " + revision10.getInternalId());
                    }
                }
            } else if ("4".equals(revision.getInternalId())) {
                // Verify revision 4
                assertEquals(1, revision.getChildren().size());
                assertEquals(1, revision.getParents().size());
                assertEquals("6", revision.getParents().get(0).getInternalId());
                assertEquals("2", revision.getChildren().get(0).getInternalId());
            } else {
                fail("Encountered unexpected revision with internal id " + revision.getInternalId());
            }
        }

        // Verify change sets
        List<DatabaseChangeSet> rev6changes = changeSetService.findAllInRevision(revisionGraphService.findRevisionInProject(project, "6"));
        assertEquals(1, rev6changes.size());
        DatabaseChangeSet initialChangeSetA = rev6changes.get(0);
        assertEquals("A", initialChangeSetA.getInternalId());
        assertNull(rev6changes.get(0).getIntroducingChangeSet());
        // TODO: Check conflicting changes -> must be multiple ones

        List<DatabaseChangeSet> rev5changes = changeSetService.findAllInRevision(revisionGraphService.findRevisionInProject(project, "5"));
        assertEquals(1, rev5changes.size());
        assertEquals("A", rev5changes.get(0).getInternalId());
        assertNotNull(rev5changes.get(0).getIntroducingChangeSet());
        assertEquals(initialChangeSetA, rev5changes.get(0).getIntroducingChangeSet());
        // TODO: Check conflicting changes -> must be multiple ones

        List<DatabaseChangeSet> rev4changes = changeSetService.findAllInRevision(revisionGraphService.findRevisionInProject(project, "4"));
        assertEquals(2, rev4changes.size());
        assertEquals("A", rev4changes.get(0).getInternalId());
        assertNotNull(rev4changes.get(0).getIntroducingChangeSet());
        assertEquals(initialChangeSetA, rev4changes.get(0).getIntroducingChangeSet());
        // TODO: Check conflicting changes -> must be multiple ones
        DatabaseChangeSet initialChangeSetB = rev4changes.get(1);
        assertEquals("B", initialChangeSetB.getInternalId());
        assertNull(initialChangeSetB.getIntroducingChangeSet());
        assertNull(initialChangeSetB.getConflictingChangeSet());

        commit();
    }
}
