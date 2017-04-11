package org.xlrnet.datac.foundation.configuration.async;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.foundation.services.EventLogService;

/**
 * Implementation of {@link AsyncUncaughtExceptionHandler} which logs events also to the database.
 */
@Component
public class LoggingAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAsyncUncaughtExceptionHandler.class);

    private final EventLogService eventLogService;

    @Autowired
    public LoggingAsyncUncaughtExceptionHandler(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        String formattedMessage = String.format("Unexpected error occurred invoking async method '%s'.", method);
        LOGGER.error(formattedMessage, ex);
        try {
            eventLogService.logException(formattedMessage, ex);
        } catch (RuntimeException e) {
            LOGGER.error("Writing event log after unexpected error failed", e);
        }
    }
}
