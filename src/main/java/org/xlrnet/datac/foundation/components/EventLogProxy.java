package org.xlrnet.datac.foundation.components;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.foundation.configuration.async.ThreadScoped;
import org.xlrnet.datac.foundation.domain.EventLog;
import org.xlrnet.datac.foundation.domain.EventLogMessage;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.session.domain.User;

/**
 * Thread-scoped proxy for {@link EventLog}. This makes it possible to use the event logging mechanism in various
 * places without that an actual event log must be present.
 * Use {@link #setDelegate(EventLog)} in order to set a event log in the current scope. If no delegate is present,
 * operations won't be executed.
 */
@Component
@ThreadScoped
public class EventLogProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventLogProxy.class);

    /** The proxied delegate. */
    private EventLog delegate;

    public boolean isDelegateSet() {
        return delegate != null;
    }

    public void setDelegate(EventLog delegate) {
        this.delegate = delegate;
    }

    public void setProject(Project project) {
        if (delegate != null) {
            delegate.setProject(project);
        } else {
            LOGGER.trace("Couldn't set project - eventlog delegate not present");
        }
    }

    public void setUser(User user) {
        if (delegate != null) {
            delegate.setUser(user);
        } else {
            LOGGER.trace("Couldn't set user - eventlog delegate not present");
        }
    }

    public void addMessage(EventLogMessage message) {
        if (delegate != null) {
            delegate.addMessage(message);
        } else {
            LOGGER.trace("Couldn't add message - eventlog delegate not present");
        }
    }

    /**
     * Returns the delegate of this proxied event log.
     * @return the delegate of this proxied event log.
     */
    public Optional<EventLog> getDelegate() {
        return Optional.ofNullable(delegate);
    }
}
