package org.xlrnet.datac.foundation.ui.components;

/**
 * Change handler which can be called when the percental progress of a task changed.
 */
@FunctionalInterface
public interface ProgressChangeHandler {

    /**
     * Handle a change in percental progress.
     * @param newProgress The new percentage progress value. Must be between 0 and 1.
     * @param newMessage  The message to display for the progress change. If null, the displayed value won't be changed.
     */
    void handleProgressChange(float newProgress, String newMessage);
}
