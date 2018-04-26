package org.xlrnet.datac.commons.lifecycle;

import org.springframework.context.SmartLifecycle;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract lifecycle component which will be started on application startup.
 */
@Slf4j
public abstract class AbstractLifecycleComponent implements SmartLifecycle {

    /** Flag if the lifecycle component is running. */
    private boolean running;

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    final public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public final void stop() {
        onStop();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public final void start() {
        try {
            onStart();
        } catch (RuntimeException e) {
            LOGGER.error("Unable to start lifecycle component", e);
            throw e;
        }
    }

    protected abstract void onStart();

    protected void onStop() {
        // Implement custom logic if necessary
    }
}
