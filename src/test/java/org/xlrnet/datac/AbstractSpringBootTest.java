package org.xlrnet.datac;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.*;

/**
 * Abstract class for tests involving Spring boot.
 */
@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public abstract class AbstractSpringBootTest {

    @Autowired
    private UserTransaction userTransaction;

    public void beginTransaction() throws SystemException, NotSupportedException {
        userTransaction.begin();
    }

    public void commit() throws HeuristicRollbackException, RollbackException, HeuristicMixedException, SystemException {
        userTransaction.commit();
    }
}
