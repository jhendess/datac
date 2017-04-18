package org.xlrnet.datac.vcs.services;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.AbstractSpringBootTest;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.test.domain.EntityCreatorUtil;
import org.xlrnet.datac.vcs.api.VcsLocalRepository;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.impl.dummy.DummyRevision;

import javax.transaction.Transactional;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ProjectUpdateService}. Tests run using {@link Transactional} to avoid errors.
 */
@Transactional
public class ProjectUpdateServiceTest extends AbstractSpringBootTest {

    @Autowired
    private ProjectUpdateService projectUpdateService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private RevisionGraphService graphService;

    private Project testProject;

    private Branch testBranch;

    @Before
    public void setupTestEntities() {
        testProject = EntityCreatorUtil.buildProject();
        testBranch = EntityCreatorUtil.buildBranch();
        testProject.addBranch(testBranch);
        testProject = projectService.save(testProject);
    }

    @Test
    public void testUpdateRevisionInBranch() throws Exception {
        DummyRevision root = buildDummyGraph();
        VcsLocalRepository mockedRepository = Mockito.mock(VcsLocalRepository.class);
        when(mockedRepository.fetchLatestRevisionInBranch(testBranch)).thenReturn(root);

        projectUpdateService.updateRevisionsInBranch(testProject, testBranch, mockedRepository);

        Revision revision = graphService.findRevisionInProject(testProject, "1");

        validateRevisionGraph(revision);
    }

    @NotNull
    private DummyRevision buildDummyGraph() {
        DummyRevision root = new DummyRevision().setInternalId("1");
        root.addParent(
                new DummyRevision().setInternalId("2")
                        .addParent(new DummyRevision().setInternalId("3"))
                        .addParent(new DummyRevision().setInternalId("4"))
        );
        return root;
    }

    @Test
    public void testUpdateRevisionInBranch_existing() throws Exception {
        // Prepare first update
        VcsLocalRepository mockedRepository = Mockito.mock(VcsLocalRepository.class);
        DummyRevision root = buildDummyGraph();
        when(mockedRepository.fetchLatestRevisionInBranch(testBranch)).thenReturn(root);
        // Perform first update
        projectUpdateService.updateRevisionsInBranch(testProject, testBranch, mockedRepository);

        // Prepare second update
        DummyRevision newRoot = new DummyRevision().setInternalId("NEW").addParent(new DummyRevision().setInternalId("1"));
        when(mockedRepository.fetchLatestRevisionInBranch(testBranch)).thenReturn(newRoot);
        // Perform second update
        projectUpdateService.updateRevisionsInBranch(testProject, testBranch, mockedRepository);

        // Validate
        Revision revision = graphService.findRevisionInProject(testProject, "NEW");
        assertNotNull(revision);
        assertEquals("New revision id doesn't match", "NEW", revision.getInternalId());
        assertEquals("New revision parent count doesn't match", 1, revision.getParents().size());
        validateRevisionGraph(revision.getParents().get(0));
    }

    @Test
    public void testFindFirstRevisionsAfterRevision() throws DatacTechnicalException {
        Revision root = new Revision().setInternalId("0");
        Revision child1 = new Revision().setInternalId("1").addParent(root);
        Revision child2 = new Revision().setInternalId("2").addParent(root);
        Revision child11 = new Revision().setInternalId("11").addParent(child1);
        Revision child21 = new Revision().setInternalId("21").addParent(child2);
        Revision child22 = new Revision().setInternalId("22").addParent(child2);

        List<Revision> firstRevisionsAfterRevision = projectUpdateService.findFirstRevisionsAfterRevision(root, Lists.newArrayList(child1, child22, child11));

        assertNotNull("Result is null", firstRevisionsAfterRevision);
        assertEquals("Expected revision size doesn't match", 2, firstRevisionsAfterRevision.size());
        assertEquals("List of revisions after revision doesn't match", Lists.newArrayList(child1, child22), firstRevisionsAfterRevision);
    }

    private void validateRevisionGraph(Revision revision) {
        assertNotNull(revision);
        assertEquals("Revision id doesn't match", "1", revision.getInternalId());
        assertEquals("Revision parent count doesn't match", 1, revision.getParents().size());
        // Validate first parent level
        Revision firstParent = revision.getParents().get(0);
        assertNotNull(firstParent);
        assertEquals("First parent revision id doesn't match", "2", firstParent.getInternalId());
        assertEquals("First parent revision parent count't doesn't match", 2, firstParent.getParents().size());
        Revision firstSecondParent = firstParent.getParents().get(0);
        assertNotNull(firstSecondParent);
        assertEquals("First second parent revision id doesn't match", "4", firstSecondParent.getInternalId());
        assertEquals("First second parent revision parent count't doesn't match", 0, firstSecondParent.getParents().size());
        Revision secondSecondParent = firstParent.getParents().get(1);
        assertNotNull(secondSecondParent);
        assertEquals("Second second parent revision id doesn't match", "3", secondSecondParent.getInternalId());
        assertEquals("Second second parent revision parent count't doesn't match", 0, secondSecondParent.getParents().size());
    }

}