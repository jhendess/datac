package org.xlrnet.datac.test.util;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Mockito answer which returns the first argument.
 */
public class ReturnFirstArgumentAnswer implements Answer<Object> {

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return args[0];
    }
}
