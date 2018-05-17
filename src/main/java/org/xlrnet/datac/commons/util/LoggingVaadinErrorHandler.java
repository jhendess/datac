package org.xlrnet.datac.commons.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.foundation.components.EventLogProxy;
import org.xlrnet.datac.foundation.domain.EventType;
import org.xlrnet.datac.foundation.services.EventLogService;
import org.xlrnet.datac.session.services.UserService;

import com.vaadin.server.ErrorEvent;
import com.vaadin.server.ErrorHandler;
import com.vaadin.ui.UI;

import de.steinwedel.messagebox.MessageBox;
import lombok.extern.slf4j.Slf4j;

/**
 * Error handler for Vaadin which logs to the event log and opens a popup if possible. If an event log proxy is already
 * available, the message will be appended to it. Otherwise a new event log will be created.
 */
@Slf4j
@Component
public class LoggingVaadinErrorHandler implements ErrorHandler {

    /** Target event log. */
    private final EventLogProxy eventLog;

    /** Service for performing logging operations.  */
    private final EventLogService eventLogService;

    /** User service which closes the current session. */
    private final UserService userService;

    @Autowired
    public LoggingVaadinErrorHandler(EventLogProxy eventLog, EventLogService eventLogService, UserService userService) {
        this.eventLog = eventLog;
        this.eventLogService = eventLogService;
        this.userService = userService;
    }

    @Override
    public void error(ErrorEvent event) {
        if (!eventLog.isDelegateSet()) {
            eventLog.setDelegate(eventLogService.newEventLog().setType(EventType.UNCAUGHT_EXCEPTION));
        }
        Throwable throwable = event.getThrowable();
        LOGGER.error("An unexpected error occurred while processing a UI action", throwable);
        eventLogService.addExceptionToEventLog(eventLog, "An unexpected error occurred while processing a UI action", throwable);
        try {
            eventLogService.save(eventLog);
            if (UI.getCurrent() != null) {
                MessageBox.createError()
                        .withCaption("Unexpected error")
                        .withHtmlMessage("An unexpected error occurred. Check the event logs for further information.<br>You will now be logged out.")
                        .withOkButton(userService::logout)
                        .open();
            }
        } catch (DatacRuntimeException r) {
            LOGGER.error("Writing error log to database failed", r);
            if (UI.getCurrent() != null) {
                MessageBox.createError()
                        .withCaption("Unexpected error")
                        .withHtmlMessage("An unexpected error occurred and writing the event log failed.<br>You will now be logged out.")
                        .withOkButton(userService::logout)
                        .open();
            }
        }
    }
}
