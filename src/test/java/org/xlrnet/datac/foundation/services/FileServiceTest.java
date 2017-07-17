package org.xlrnet.datac.foundation.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.xlrnet.datac.foundation.domain.Project;

/**
 * Tests for {@link FileService}
 */
public class FileServiceTest {

    private static final String BASE_PATH = "/tmp/datac";

    private FileService fileService;

    @Before
    public void setup() {
        fileService = spy(new FileService());
        when(fileService.getWorkingDirectoryPath()).thenReturn(Paths.get(BASE_PATH));
    }

    @Test
    public void prepareProjectRepositoryPath() throws Exception {
    }

    @Test
    public void getProjectRepositoryPath() throws Exception {
        Project project = new Project();
        project.setId(123456789L);

        Path projectRepositoryPath = fileService.getProjectRepositoryPath(project);
        assertEquals(projectRepositoryPath, Paths.get(BASE_PATH, "123456789"));
    }
}