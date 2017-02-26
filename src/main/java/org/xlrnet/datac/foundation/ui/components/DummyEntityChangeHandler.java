package org.xlrnet.datac.foundation.ui.components;

/**
 * Empty implementation of {@link EntityChangeHandler} which does nothing.
 */
public class DummyEntityChangeHandler<T> implements EntityChangeHandler<T> {

    @Override
    public void onChange(T entity) {
        // Do nothing
    }
}
