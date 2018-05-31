package org.xlrnet.datac.foundation.ui.util;

import org.xlrnet.datac.foundation.ui.components.ProgressChangeHandler;
import org.xlrnet.datac.foundation.ui.components.ProgressWindow;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link ProgressChangeHandler} which updates a {@link ProgressWindow}.
 */
@Slf4j
public class ProgressWindowChangeHandler implements ProgressChangeHandler {

    /** The window to update. */
    private final ProgressWindow progressWindow;

    public ProgressWindowChangeHandler(ProgressWindow progressWindow) {
        this.progressWindow = progressWindow;
    }

    @Override
    public void handleProgressChange(float newProgress, String newMessage) {
        LOGGER.trace("Received progress update: newProgress={}, newMessage={}", newProgress, newMessage);
        progressWindow.getUI().access(() -> {
            progressWindow.updateProgress(newProgress);
            if (newMessage != null) {
                progressWindow.setMessage(newMessage);
            }
        });
    }
}
