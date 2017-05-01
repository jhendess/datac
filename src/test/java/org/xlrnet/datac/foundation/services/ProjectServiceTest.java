package org.xlrnet.datac.foundation.services;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.spring.events.EventBus;
import org.xlrnet.datac.foundation.components.EventLogProxy;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.repository.ProjectRepository;
import org.xlrnet.datac.test.util.ReturnFirstArgumentAnswer;
import org.xlrnet.datac.vcs.api.VcsLocalRepository;
import org.xlrnet.datac.vcs.api.VcsRemoteRepositoryConnection;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.services.BranchService;
import org.xlrnet.datac.vcs.services.ProjectSchedulingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ProjectService}.
 */
public class ProjectServiceTest {

    private static final String NEW_BRANCH_PATTERN = "[1-9]+\\.[0-9]+\\.x";

    private ProjectService projectService;

    private FileService fileService;

    private ProjectRepository repository;

    private VcsLocalRepository localRepositoryMock;

    private VcsRemoteRepositoryConnection remoteRepositoryMock;

    private Branch existingBranch;

    private Branch newBranch;

    @Before
    public void setUp() throws Exception {
        // Configure mocks
        localRepositoryMock = mock(VcsLocalRepository.class);
        remoteRepositoryMock = mock(VcsRemoteRepositoryConnection.class);
        when(localRepositoryMock.connectToRemote()).thenReturn(remoteRepositoryMock);
        repository = mock(ProjectRepository.class);
        when(repository.save(any(Project.class))).thenAnswer(new ReturnFirstArgumentAnswer());
        fileService = mock(FileService.class);
        projectService = new ProjectService(repository, mock(EventBus.ApplicationEventBus.class), fileService, new EventLogProxy(), mock(BranchService.class), mock(ProjectSchedulingService.class));

        // Configure branch dummies
        existingBranch = new Branch();
        existingBranch.setName("1.0.x");
        existingBranch.setInternalId("abcdef");
        existingBranch.setWatched(true);

        newBranch = new Branch();
        newBranch.setName("2.0.x");
        newBranch.setInternalId("ghijkl");

        when(remoteRepositoryMock.listBranches()).thenReturn(Lists.newArrayList(existingBranch, newBranch));
    }

    @Test
    public void testUpdateAvailableBranches_matches() throws Exception {
        Project project = new Project();
        project.addBranch(existingBranch);
        project.setNewBranchPattern(NEW_BRANCH_PATTERN);

        Project updated = projectService.updateAvailableBranches(project, localRepositoryMock);

        assertEquals(2, updated.getBranches().size());
        assertThat(updated.getBranches()).containsOnlyOnce(newBranch, existingBranch);
        for (Branch branch : updated.getBranches()) {
            assertTrue("All branches must be watched", branch.isWatched());
        }
    }

    @Test
    public void testUpdateAvailableBranches_noMatches() throws Exception {
        Project project = new Project();
        project.addBranch(existingBranch);
        project.setNewBranchPattern("xyz");

        Project updated = projectService.updateAvailableBranches(project, localRepositoryMock);

        assertEquals(2, updated.getBranches().size());
        assertThat(updated.getBranches()).containsOnlyOnce(newBranch, existingBranch);
        assertFalse(newBranch.isWatched());
    }

}