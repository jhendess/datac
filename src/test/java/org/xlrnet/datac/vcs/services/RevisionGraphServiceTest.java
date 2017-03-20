package org.xlrnet.datac.vcs.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.ConstraintViolationException;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.AbstractSpringBootTest;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;

/**
 * Tests simple CRUD operations on the revision graph.
 */
public class RevisionGraphServiceTest extends AbstractSpringBootTest {

    @Autowired
    private RevisionGraphService revisionGraphService;

    @Autowired
    private ProjectService projectService;

    private Project testProject;

    @Before
    public void setupTestEntities() {
        testProject = buildProject();

        Branch testBranch = buildBranch();
        testProject.addBranch(testBranch);

        testProject = projectService.save(testProject);
    }

    @NotNull
    private Branch buildBranch() {
        Branch testBranch = new Branch();
        testBranch.setName("1");
        testBranch.setDevelopment(true);
        testBranch.setInternalId("1");
        return testBranch;
    }

    private Project buildProject() {
        Project p = new Project();
        p.setChangelogLocation("/");
        p.setAdapterClass("TEST");
        p.setType("TEST");
        p.setUrl("Some_URL");
        p.setNewBranchPattern(".*");
        p.setName("TEST");
        return p;
    }

    @Test
    public void testSaveAndRead() {
        Revision child = new Revision()
                .setInternalId("child")
                .setAuthor("someAuthor")
                .setCommitter("someCommitter")
                .setCommitTime(LocalDateTime.now())
                .setMessage("This is a child")
                .setProject(testProject);
        Revision parent = new Revision()
                .setInternalId("parent")
                .setAuthor("someAuthor")
                .setCommitter("someCommitter")
                .setCommitTime(LocalDateTime.now())
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
                .setCommitter("someCommitter")
                .setCommitTime(LocalDateTime.now())
                .setMessage("This is a child")
                .setProject(testProject);
        Revision parent = new Revision()
                .setInternalId("parent")
                .setAuthor("someAuthor")
                .setCommitter("someCommitter")
                .setCommitTime(LocalDateTime.now())
                .setMessage("This is a parent")
                .setProject(project2);

        child.addParent(parent);
        revisionGraphService.save(child);
    }

}