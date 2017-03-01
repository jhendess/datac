package org.xlrnet.datac.commons.tasks;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.foundation.ui.components.BooleanStatusChangeHandler;
import org.xlrnet.datac.foundation.ui.components.EntityChangeHandler;

/**
 * Abstract implementation of {@link RunnableTask}.
 */
public abstract class AbstractRunnableTask<T> implements RunnableTask<T> {

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
}
