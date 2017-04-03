package org.xlrnet.datac.foundation.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.Instant;

/**
 * A single log message in a {@link EventLog}.
 */
@Entity
@Table(name = "eventlog_message")
public class EventLogMessage extends AbstractEntity {

    @NotNull
    @Column(name = "short_message")
    private String shortMessage;

    @Column(name = "detailed_message")
    private String detailedMessage;

    @NotNull
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @NotNull
    @Column(name = "severity")
    private MessageSeverity severity = MessageSeverity.INFO;

    public String getShortMessage() {
        return shortMessage;
    }

    public EventLogMessage setShortMessage(String shortMessage) {
        this.shortMessage = shortMessage;
        return this;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public EventLogMessage setDetailedMessage(String detailedMessage) {
        this.detailedMessage = detailedMessage;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public EventLogMessage setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public MessageSeverity getSeverity() {
        return severity;
    }

    public EventLogMessage setSeverity(MessageSeverity severity) {
        this.severity = severity;
        return this;
    }
}
