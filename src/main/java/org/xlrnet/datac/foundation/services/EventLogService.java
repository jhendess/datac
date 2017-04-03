package org.xlrnet.datac.foundation.services;

import com.google.common.base.Throwables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.foundation.domain.EventLog;
import org.xlrnet.datac.foundation.domain.EventLogMessage;
import org.xlrnet.datac.foundation.domain.EventType;
import org.xlrnet.datac.foundation.domain.MessageSeverity;
import org.xlrnet.datac.foundation.domain.repository.EventLogRepository;

import java.time.Instant;

/**
 * Service for high-level event logging. Provides convenient methods for creating and reading event logs.
 */
@Service
public class EventLogService extends AbstractTransactionalService<EventLog, EventLogRepository> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventLogService.class);

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *
     * @param crudRepository
     *         The crud repository for providing basic crud operations.
     */
    public EventLogService(EventLogRepository crudRepository) {
        super(crudRepository);
    }

    /***
     * Creates a new {@link EventLog} object.
     *
     * @return A new event log object.
     */
    @NotNull
    @Transactional(propagation = Propagation.SUPPORTS)
    public EventLog newEventLog() {
        return new EventLog().setCreated(Instant.now());
    }

    /**
     * Saves the given event log in a separate transaction.
     *
     * @param eventLog
     *         The event log to save.
     * @return The persisted event log.
     */
    @NotNull
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventLog save(@NotNull EventLog eventLog) {
        try {
            return super.save(eventLog);
        } catch (RuntimeException e) {
            LOGGER.error("Writing eventlog {} failed", eventLog, e);
            throw new DatacRuntimeException("Writing eventlog failed", e);
        }
    }

    /**
     * Create and log a simple event log with just a single message. Will be executed in a separate transaction.
     *
     * @param type
     *         Type of the {@link EventLogMessage} in the new {@link EventLog}.
     * @param severity
     *         Severity of the {@link EventLogMessage}.
     * @param message
     *         The message to log.
     * @param details
     *         Detailed message to log.
     * @return The persisted event log.
     */
    @NotNull
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventLog logSimpleEventMessage(@NotNull EventType type, @NotNull MessageSeverity severity, @NotNull String message, @Nullable String details) {
        EventLog eventLog = newEventLog()
                .setType(type)
                .addMessage(
                        new EventLogMessage()
                                .setSeverity(severity)
                                .setShortMessage(message)
                                .setDetailedMessage(details)
                );
        return save(eventLog);
    }

    /**
     * Create and log a simple event log with just a single message. Will be executed in a separate transaction.
     *
     * @param type
     *         Type of the {@link EventLogMessage} in the new {@link EventLog}.
     * @param severity
     *         Severity of the {@link EventLogMessage}.
     * @param message
     *         The message to log.
     * @return The persisted event log.
     */
    public EventLog logSimpleEventMessage(@NotNull EventType type, @NotNull MessageSeverity severity, @NotNull String message) {
        return logSimpleEventMessage(type, severity, message, null);
    }

    /**
     * Log any exception to a new {@link EventLog}. Will be executed in a separate transaction.
     *
     * @param message
     *         Custom message to log.
     * @param exception
     *         The exception to log.
     * @return The persisted event log.
     */
    @NotNull
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventLog logException(@NotNull String message, @NotNull Throwable exception) {
        EventLog eventLog = newEventLog();
        addExceptionToEventLog(eventLog, message, exception);
        return save(eventLog);
    }

    /**
     * Adds an exception to an existing {@link EventLog}.
     *
     * @param targetLog
     *         The event log to which the exception should be logged.
     * @param message
     *         Custom message to log.
     * @param exception
     *         The exception to log.
     * @return The given event log.
     */
    @NotNull
    @Transactional(propagation = Propagation.SUPPORTS)
    public EventLog addExceptionToEventLog(@NotNull EventLog targetLog, @NotNull String message, @NotNull Throwable exception) {
        EventLogMessage eventLogMessage = new EventLogMessage()
                .setSeverity(MessageSeverity.ERROR)
                .setShortMessage(message)
                .setDetailedMessage(Throwables.getStackTraceAsString(exception));
        return targetLog.addMessage(eventLogMessage);
    }
}
