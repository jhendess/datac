package org.xlrnet.datac.foundation.ui.components;

/**
 * Change handler will be called when a boolean status changes. This can e.g. be used as a handler for running/not
 * running state changes.
 */
@FunctionalInterface
public interface BooleanStatusChangeHandler {

    /**
     * Method will be called when a boolean status has changed.
     * @param newStatus new boolean status.
     */
    void handleStatusChange(boolean newStatus);
}
