package org.xlrnet.datac.commons.tasks;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlrnet.datac.foundation.ui.components.BooleanStatusChangeHandler;
import org.xlrnet.datac.foundation.ui.components.EntityChangeHandler;

/**
 * Abstract implementation of {@link RunnableTask}. It provides a simple implementation of {@link #run()} with automatic
 * calling of the given {@link BooleanStatusChangeHandler}.
 */
public abstract class AbstractRunnableTask<T> implements RunnableTask<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRunnableTask.class);

    /** Handler which will be called when the running state of this task changes. */
    private BooleanStatusChangeHandler runningStatusHandler = (b) -> {};

    /** Handler which will be called when the connection check has finished. */
    private EntityChangeHandler<T> entityChangeHandler = (s) -> {};

    protected BooleanStatusChangeHandler getRunningStatusHandler() {
        return runningStatusHandler;
    }

    protected EntityChangeHandler<T> getEntityChangeHandler() {
        return entityChangeHandler;
    }

    @Override
    public void setRunningStatusHandler(@NotNull BooleanStatusChangeHandler runningStatusHandler) {
        this.runningStatusHandler = runningStatusHandler;
    }

    @Override
    public void setEntityChangeHandler(@NotNull EntityChangeHandler<T> entityChangeHandler) {
        this.entityChangeHandler = entityChangeHandler;
    }

    @Override
    public void run() {
        getRunningStatusHandler().handleStatusChange(true);
        try {
            runTask();
        } catch (Exception e) {
            LOGGER.error("Task failed due to unexpected exception", e);
        } finally {
            getRunningStatusHandler().handleStatusChange(false);
        }
    }

    /**
     * Method containing the actual logic performed by this task.
     */
    protected abstract void runTask();
}
