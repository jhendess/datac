package org.xlrnet.datac.commons.tasks;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.foundation.ui.components.BooleanStatusChangeHandler;
import org.xlrnet.datac.foundation.ui.components.EntityChangeHandler;

/**
 * Runnable task which can be used to perform long-running background task on the user interface.
 *
 * @param <T> Type of entity that will be returned by a specific task
 */
public interface RunnableTask<T> extends Runnable {


    /**
     * Sets a handler which will be called when the running state of the task changes. This can be used e.g. be used to
     * update UI components before and after the check starts.
     *
     * @param runningStatusHandler The handler to call when the running state changes.
     */
    void setRunningStatusHandler(@NotNull BooleanStatusChangeHandler runningStatusHandler);

    /**
     * Sets a handler which will be called when the task has finished and a connection result is available.
     *
     * @param entityChangeHandler The handler to call when the connection state changes.
     */
    void setEntityChangeHandler(@NotNull EntityChangeHandler<T> entityChangeHandler);
}
