package org.xlrnet.datac.foundation.configuration.async;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Factory for creating instances of {@link TaskExecutor}.
 */
@Component
public class TaskExecutorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutorFactory.class);

    /**
     * Size of the default pool.
     */
    private static final int DEFAULT_POOL_SIZE = 20;

    /**
     * Queue capacity for the default pool.
     */
    private static final int DEFAULT_QUEUE_CAPACITY = 20;

    private ThreadPoolTaskExecutor defaultExecutor;

    @PostConstruct
    void init() {
        defaultExecutor = new ThreadPoolTaskExecutor();
        defaultExecutor.setCorePoolSize(DEFAULT_POOL_SIZE);
        defaultExecutor.setMaxPoolSize(DEFAULT_POOL_SIZE);
        defaultExecutor.setQueueCapacity(DEFAULT_QUEUE_CAPACITY);
        defaultExecutor.setThreadNamePrefix("defTaskExec-");
        LOGGER.info("Initializing default TaskExecutor with {} threads and queue size of {}", DEFAULT_POOL_SIZE, DEFAULT_QUEUE_CAPACITY);
        defaultExecutor.initialize();
    }

    /**
     * Bean producer method for the default task executor.
     *
     * @return The default task executor.
     */
    @Bean
    public TaskExecutor getDefaultTaskExecutor() {
        return defaultExecutor;
    }
}