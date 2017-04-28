package org.xlrnet.datac.foundation.configuration;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.xlrnet.datac.foundation.configuration.async.UncaughtExceptionHandler;

/**
 * Configuration to enable task scheduling.
 */
@Configuration
@EnableScheduling
public class SchedulerConfiguration {

    private final UncaughtExceptionHandler exceptionHandler;

    private final TaskExecutor taskExecutor;

    private ConcurrentTaskScheduler scheduler;

    @Autowired
    public SchedulerConfiguration(UncaughtExceptionHandler exceptionHandler, @Qualifier("defaultTaskExecutor") TaskExecutor taskExecutor) {
        this.exceptionHandler = exceptionHandler;
        this.taskExecutor = taskExecutor;
    }

    @PostConstruct
    private void init() {
        scheduler = new ConcurrentTaskScheduler();
        scheduler.setErrorHandler(exceptionHandler);
        scheduler.setConcurrentExecutor(taskExecutor);
    }

    @Bean
    public TaskScheduler defaultTaskScheduler() {
        return scheduler;
    }
}
