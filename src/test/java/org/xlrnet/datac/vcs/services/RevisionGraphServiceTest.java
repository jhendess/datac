package org.xlrnet.datac.vcs.services;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.AbstractSpringBootTest;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.test.domain.EntityCreatorUtil;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;

import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.xlrnet.datac.test.domain.EntityCreatorUtil.buildBranch;
import static org.xlrnet.datac.test.domain.EntityCreatorUtil.buildProject;

/**
 * Tests simple CRUD operations on the revision graph.  Tests run using {@link Transactional} to avoid errors.
 */
@Transactional
public class RevisionGraphServiceTest extends AbstractSpringBootTest {

    @Autowired
    private RevisionGraphService revisionGraphService;

    @Autowired
    private ProjectService projectService;

    private Project testProject;

    @Before
    public void setupTestEntities() {
        testProject = EntityCreatorUtil.buildProject();

        Branch testBranch = EntityCreatorUtil.buildBranch();
        testProject.addBranch(testBranch);

        testProject = projectService.save(testProject);
    }

    @Test
    public void testSaveAndRead() {
        Revision child = new Revision()
                .setInternalId("child")
                .setAuthor("someAuthor")
                .setReviewer("someCommitter")
                .setCommitTime(Instant.now())
                .setMessage("This is a child")
                .setProject(testProject);
        Revision parent = new Revision()
                .setInternalId("parent")
                .setAuthor("someAuthor")
                .setReviewer("someCommitter")
                .setCommitTime(Instant.now())
                .setMessage("This is a parent")
                .setProject(testProject);

        child.addParent(parent);

        revisionGraphService.save(child);

        List<Revision> revisions = revisionGraphService.findAllByProject(testProject);
        assertEquals(2, revisions.size());
        for (Revision revision : revisions) {
            assertNotNull("Expected project to be set", revision.getProject());
            assertEquals(testProject.getId(), revision.getProject().getId());
            if ("child".equals(revision.getInternalId())) {
                assertEquals(1, revision.getParents().size());
            } else {
                assertEquals(0, revision.getParents().size());
            }
        }
    }

    @Test(expected = ConstraintViolationException.class)
    public void testDifferentProjects() {
        Project project2 = buildProject();
        Branch branch2 = buildBranch();

        projectService.save(project2);

        Revision child = new Revision()
                .setInternalId("child")
                .setAuthor("someAuthor")
                .setReviewer("someCommitter")
                .setCommitTime(Instant.now())
                .setMessage("This is a child")
                .setProject(testProject);
        Revision parent = new Revision()
                .setInternalId("parent")
                .setAuthor("someAuthor")
                .setReviewer("someCommitter")
                .setCommitTime(Instant.now())
                .setMessage("This is a parent")
                .setProject(project2);

        child.addParent(parent);
        revisionGraphService.save(child);
    }

    @Test
    public void testSaveStackOverflow() {
        Revision newRoot = new Revision().setInternalId("0").setProject(testProject).setCommitTime(Instant.now());
        Revision revision = newRoot;


        for (int i = 1; i < 1000; i++) {
            Revision parent = new Revision().setInternalId(Integer.toString(i)).setCommitTime(Instant.now()).setProject(testProject);
            revision.addParent(parent);
            revision = parent;
        }

        revisionGraphService.save(newRoot);
    }

    @Test
    public void testSaveMultipleParents() {
        Revision root = new Revision().setInternalId("0").setProject(testProject).setCommitTime(Instant.now());

        root.addParent(new Revision().setInternalId("1").setProject(testProject).setCommitTime(Instant.now()));
        root.addParent(new Revision().setInternalId("2").setProject(testProject).setCommitTime(Instant.now()));

        revisionGraphService.save(root);

        Revision savedRevision = revisionGraphService.findRevisionInProject(testProject, "0");
        assertNotNull(savedRevision);
        assertEquals(2, savedRevision.getParents().size());
    }

    @Test
    public void testSaveMultipleParentsSameRoot() {
        Revision root = new Revision().setInternalId("0").setProject(testProject).setCommitTime(Instant.now());

        Revision parent1 = new Revision().setInternalId("1").setProject(testProject).setCommitTime(Instant.now());
        Revision parent2 = new Revision().setInternalId("2").setProject(testProject).setCommitTime(Instant.now());
        Revision oldestParent = new Revision().setInternalId("3").setProject(testProject).setCommitTime(Instant.now());

        root.addParent(parent1);
        root.addParent(parent2);

        parent1.addParent(oldestParent);
        parent2.addParent(oldestParent);

        revisionGraphService.save(root);

        Revision savedRevision = revisionGraphService.findRevisionInProject(testProject, "0");
        assertNotNull(savedRevision);
        assertEquals(2, savedRevision.getParents().size());
    }

    @Test
    public void testFindRootRevision() {
        Revision root = new Revision().setInternalId("0").setProject(testProject).setCommitTime(Instant.now());
        Revision parent1 = new Revision().setInternalId("1").setProject(testProject).setCommitTime(Instant.now());
        root.addParent(parent1);

        revisionGraphService.save(root);
        Revision projectRootRevision = revisionGraphService.findProjectRootRevision(testProject);

        assertNotNull(projectRootRevision);
        assertEquals("1", projectRootRevision.getInternalId());
    }

}