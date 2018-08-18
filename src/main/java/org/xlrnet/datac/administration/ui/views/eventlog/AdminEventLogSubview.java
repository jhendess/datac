package org.xlrnet.datac.administration.ui.views.eventlog;

import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.xlrnet.datac.administration.services.ApplicationMaintenanceService;
import org.xlrnet.datac.session.ui.views.AbstractSubview;

/**
 * Admin view for viewing event logs.
 */
@UIScope
@SpringView(name = AdminEventLogSubview.VIEW_NAME)
public class AdminEventLogSubview extends AbstractSubview {

    private final EventLogLayout eventLogLayout;

    public static final String VIEW_NAME = "admin/eventlog";

    @Autowired
    public AdminEventLogSubview(EventBus.ApplicationEventBus applicationEventBus, ApplicationMaintenanceService maintenanceService, EventLogLayout eventLogLayout) {
        super(applicationEventBus, maintenanceService);
        this.eventLogLayout = eventLogLayout;
    }

    @Override
    protected void initialize() {
        // Nothing to do
    }

    @NotNull
    @Override
    protected String getTitle() {
        return "Event Log";
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        return "Monitor application events";
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        return eventLogLayout;
    }
}
