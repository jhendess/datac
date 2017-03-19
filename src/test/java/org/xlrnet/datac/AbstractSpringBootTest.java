package org.xlrnet.datac;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Abstract class for tests involving Spring boot.
 */
@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public abstract class AbstractSpringBootTest {

}
