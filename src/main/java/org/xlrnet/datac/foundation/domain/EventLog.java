package org.xlrnet.datac.foundation.domain;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import org.jetbrains.annotations.Nullable;
import org.xlrnet.datac.session.domain.User;

/**
 * Eventlog which consists of logged event messages.
 */
@Entity
@Table(name = "eventlog")
public class EventLog extends AbstractEntity {

    /**
     * The type of event to log.
     */
    @NotNull
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private EventType type;

    /**
     * The time when the event log was created.
     */
    @NotNull
    @Column(name = "created")
    private Instant created;

    /**
     * The project to which the event log belongs (if any).
     */
    @JoinColumn(name = "project_id")
    @OneToOne(cascade = {CascadeType.REMOVE, CascadeType.REFRESH}, fetch = FetchType.EAGER)
    private Project project;

    /**
     * The user who caused the event to occur (if any).
     */
    @JoinColumn(name = "user_id")
    @OneToOne(cascade = {CascadeType.REMOVE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    private User user;

    /**
     * The messages in the event log.
     */
    @OneToMany(cascade = CascadeType.ALL, targetEntity = EventLogMessage.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "eventlog_id", referencedColumnName = "id", nullable = false)
    private List<EventLogMessage> messages = new LinkedList<>();

    public EventType getType() {
        return type;
    }

    public EventLog setType(EventType type) {
        this.type = type;
        return this;
    }

    public Instant getCreated() {
        return created;
    }

    public EventLog setCreated(Instant created) {
        this.created = created;
        return this;
    }

    public Project getProject() {
        return project;
    }

    public EventLog setProject(Project project) {
        this.project = project;
        return this;
    }

    public User getUser() {
        return user;
    }

    public EventLog setUser(User user) {
        this.user = user;
        return this;
    }

    public EventLog addMessage(EventLogMessage message) {
        if (message != null && !this.messages.contains(message)) {
            this.messages.add(message);
            message.setEventLog(this);
        }
        return this;
    }

    public int getMessageCount() {
        return this.messages.size();
    }

    public MessageSeverity getHighestMessageSeverity() {
        MessageSeverity highestSeverity = MessageSeverity.INFO;
        for (EventLogMessage message : messages) {
            if (message.getSeverity().getSeverityLevel() > highestSeverity.getSeverityLevel()) {
                highestSeverity = message.getSeverity();
            }
        }
        return highestSeverity;
    }

    public List<EventLogMessage> getMessages() {
        return messages;
    }

    public EventLog setMessages(List<EventLogMessage> messages) {
        this.messages = messages;
        return this;
    }

    @Nullable
    public String getProjectName() {
        return getProject() != null ? getProject().getName() : null;
    }

    @Nullable
    public String getUserName() {
        return getUser() != null ? getUser().getFirstName() : null;
    }
}
