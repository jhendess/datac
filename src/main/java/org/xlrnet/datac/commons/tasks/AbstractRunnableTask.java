package org.xlrnet.datac.commons.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlrnet.datac.foundation.ui.components.BooleanStatusChangeHandler;
import org.xlrnet.datac.foundation.ui.components.EntityChangeHandler;
import org.xlrnet.datac.foundation.ui.components.ProgressChangeHandler;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract implementation of {@link RunnableTask}. It provides a simple implementation of {@link #run()} with automatic
 * calling of the given {@link BooleanStatusChangeHandler} and {@link ProgressChangeHandler}.
 */
public abstract class AbstractRunnableTask<T> implements RunnableTask<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRunnableTask.class);

    /** Handler which will be called when the running state of this task changes. */
    @Setter
    @Getter(AccessLevel.PROTECTED)
    private BooleanStatusChangeHandler runningStatusHandler = (b) -> {};

    /** Handler which will be called when the connection check has finished. */
    @Setter
    @Getter(AccessLevel.PROTECTED)
    private EntityChangeHandler<T> entityChangeHandler = (s) -> {};

    /** Handler which will be called on a progress change. */
    @Setter
    @Getter(AccessLevel.PROTECTED)
    private ProgressChangeHandler progressChangeHandler = (p, m) -> {};

    @Override
    public void run() {
        getRunningStatusHandler().handleStatusChange(true);
        getProgressChangeHandler().handleProgressChange(0, null);
        try {
            runTask();
        } catch (Exception e) {
            LOGGER.error("Task failed with an unexpected exception", e);
        } finally {
            getRunningStatusHandler().handleStatusChange(false);
            getProgressChangeHandler().handleProgressChange(1, null);
        }
    }

    /**
     * Method containing the actual logic performed by this task.
     */
    protected abstract void runTask();
}
