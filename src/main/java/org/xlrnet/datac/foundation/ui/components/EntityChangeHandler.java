package org.xlrnet.datac.foundation.ui.components;

/**
 * Simple change handler that will be called when the currently edited object is either saved or deleted.
 */
@FunctionalInterface
public interface EntityChangeHandler<T> {

    /**
     * Method will be called when the object which is currently being edited is changed.
     * @param entity The entity that is being changed.
     */
    void onChange(T entity);
}
