package org.xlrnet.datac.foundation.configuration.async;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution using {@link EnableAsync}.
 */
@EnableAsync
@Configuration
public class AsyncConfiguration implements AsyncConfigurer {

    private final TaskExecutorFactory taskExecutorFactory;

    private final UncaughtExceptionHandler asyncUncaughtExceptionHandler;

    @Autowired
    public AsyncConfiguration(TaskExecutorFactory taskExecutorFactory, UncaughtExceptionHandler asyncUncaughtExceptionHandler) {
        this.taskExecutorFactory = taskExecutorFactory;
        this.asyncUncaughtExceptionHandler = asyncUncaughtExceptionHandler;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutorFactory.defaultTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return asyncUncaughtExceptionHandler;
    }
}
