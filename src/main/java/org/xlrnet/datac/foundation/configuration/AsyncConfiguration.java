package org.xlrnet.datac.foundation.configuration;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for asynchronous task execution using {@link EnableAsync}.
 */
@EnableAsync
@Configuration
public class AsyncConfiguration implements AsyncConfigurer {

    private final TaskExecutorFactory taskExecutorFactory;

    @Autowired
    public AsyncConfiguration(TaskExecutorFactory taskExecutorFactory) {
        this.taskExecutorFactory = taskExecutorFactory;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutorFactory.getDefaultTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
