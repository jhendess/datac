package org.xlrnet.datac.foundation.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Factory for creating instances of {@link TaskExecutor}.
 */
@Component
public class TaskExecutorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutorFactory.class);

    private ThreadPoolTaskExecutor defaultExecutor;

    @PostConstruct
    void init() {
        LOGGER.info("Initializing default TaskExecutor");
        defaultExecutor = new ThreadPoolTaskExecutor();
        defaultExecutor.setCorePoolSize(10);
        defaultExecutor.setMaxPoolSize(40);
        defaultExecutor.setQueueCapacity(20);
        defaultExecutor.setThreadNamePrefix("defTaskExec-");
        defaultExecutor.initialize();
    }

    /**
     *
     * @return
     */
    @Bean
    public TaskExecutor getDefaultTaskExecutor() {
        return defaultExecutor;
    }
}