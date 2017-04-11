package org.xlrnet.datac.foundation.configuration.async;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for asynchronous task execution using {@link EnableAsync}.
 */
//@EnableAsync
//@Configuration
public class AsyncConfiguration implements AsyncConfigurer {

    private final TaskExecutorFactory taskExecutorFactory;

    private final LoggingAsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler;

    @Autowired
    public AsyncConfiguration(TaskExecutorFactory taskExecutorFactory, LoggingAsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler) {
        this.taskExecutorFactory = taskExecutorFactory;
        this.asyncUncaughtExceptionHandler = asyncUncaughtExceptionHandler;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutorFactory.getDefaultTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return asyncUncaughtExceptionHandler;
    }
}
