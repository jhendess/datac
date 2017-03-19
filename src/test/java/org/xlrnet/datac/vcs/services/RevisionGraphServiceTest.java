package org.xlrnet.datac.vcs.services;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.AbstractSpringBootTest;
import org.xlrnet.datac.vcs.domain.Revision;

import java.time.LocalDateTime;

/**
 * Tests simple CRUD operations on the revision graph.
 */
public class RevisionGraphServiceTest extends AbstractSpringBootTest {

    @Autowired
    private RevisionGraphService revisionGraphService;

    @Test
    public void testCreate() {
        Revision child = new Revision()
                .setInternalId("child")
                .setAuthor("someAuthor")
                .setCommitter("someCommitter")
                .setCommitTime(LocalDateTime.now())
                .setMessage("This is a child");
        Revision parent = new Revision()
                .setInternalId("parent")
                .setAuthor("someAuthor")
                .setCommitter("someCommitter")
                .setCommitTime(LocalDateTime.now())
                .setMessage("This is a parent");

        child.addParent(parent);
        revisionGraphService.save(child);
    }

}