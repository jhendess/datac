package org.xlrnet.datac.foundation.configuration.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;
import org.xlrnet.datac.foundation.services.EventLogService;

import java.lang.reflect.Method;

/**
 * Implementation of {@link AsyncUncaughtExceptionHandler} which logs events also to the database.
 */
@Component
public class UncaughtExceptionHandler implements AsyncUncaughtExceptionHandler, ErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UncaughtExceptionHandler.class);

    private final EventLogService eventLogService;

    @Autowired
    public UncaughtExceptionHandler(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        String formattedMessage = String.format("Unexpected error occurred invoking async method '%s'.", method);
        logException(ex, formattedMessage);
    }

    @Override
    public void handleError(Throwable t) {
        logException(t, "An unexpected error occurred");
    }

    private void logException(Throwable ex, String formattedMessage) {
        LOGGER.error(formattedMessage, ex);
        try {
            eventLogService.logException(formattedMessage, ex);
        } catch (RuntimeException e) {
            LOGGER.error("Writing event log after unexpected error failed", e);
        }
    }
}
