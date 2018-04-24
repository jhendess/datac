package org.xlrnet.datac.foundation.services;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.xlrnet.datac.AbstractSpringBootTest;
import org.xlrnet.datac.foundation.components.EventLogProxy;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.PasswordEncryptionListener;
import org.xlrnet.datac.foundation.domain.repository.ProjectRepository;
import org.xlrnet.datac.session.services.CryptoService;
import org.xlrnet.datac.test.domain.EntityCreatorUtil;
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
public class ProjectServiceTest extends AbstractSpringBootTest {

    private static final String NEW_BRANCH_PATTERN = "[1-9]+\\.[0-9]+\\.x";

    private static final String SAMPLE_PASSWORD = "MY_HIDDEN_PASSWORD_WITH_MAXIMUM_ALLOWED_CHARACTERS";

    /**
     * Manually mocked project service.
     */
    private ProjectService projectServiceMocked;

    /**
     * The real service.
     */
    @Autowired
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
        projectServiceMocked = new ProjectService(repository, mock(EventBus.ApplicationEventBus.class), fileService, new EventLogProxy(), mock(BranchService.class), mock(ProjectSchedulingService.class), new PasswordEncryptionListener(new CryptoService()));

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
    public void testTransparentPasswordEncryption() {
        Project project = EntityCreatorUtil.buildProject();
        project.setPassword(SAMPLE_PASSWORD);
        project.addBranch(EntityCreatorUtil.buildBranch());

        Project saved = projectService.save(project);
        assertNotNull("Encrypted password may not be empty", saved.getEncryptedPassword());
        assertNotNull("Password salt may not be empty", saved.getSalt());

        Project loaded = projectService.findOne(project.getId());
        assertEquals("Passwords don't match after decryption", SAMPLE_PASSWORD, loaded.getPassword());
    }

    @Test
    public void testTransparentPasswordEncryption_onlyPWUpdate() {
        Project project = EntityCreatorUtil.buildProject();
        project.setPassword(SAMPLE_PASSWORD);
        project.addBranch(EntityCreatorUtil.buildBranch());

        projectService.save(project);

        Project loaded = projectService.findOne(project.getId());
        loaded.setPassword("NEW_PASSWORD");
        Project saved = projectService.save(loaded);

        assertEquals("Passwords don't match after updating", "NEW_PASSWORD", saved.getPassword());
    }

    @Test
    public void testUpdateAvailableBranches_matches() throws Exception {
        Project project = new Project();
        project.addBranch(existingBranch);
        project.setNewBranchPattern(NEW_BRANCH_PATTERN);

        Project updated = projectServiceMocked.updateAvailableBranches(project, localRepositoryMock);

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

        Project updated = projectServiceMocked.updateAvailableBranches(project, localRepositoryMock);

        assertEquals(2, updated.getBranches().size());
        assertThat(updated.getBranches()).containsOnlyOnce(newBranch, existingBranch);
        assertFalse(newBranch.isWatched());
    }

}