package org.xlrnet.datac.administration.util;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

/**
 * Application event to inform all components about the begin or end of the maintenance mode.
 */
public class MaintenanceModeEvent extends ApplicationEvent {

    @Getter
    private final boolean maintenanceModeStatus;

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public MaintenanceModeEvent(Object source, boolean maintenanceModeStatus) {
        super(source);
        this.maintenanceModeStatus = maintenanceModeStatus;
    }
}
