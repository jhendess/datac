package org.xlrnet.datac.administration.ui.views.eventlog;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.foundation.ui.views.AbstractSubview;

import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;

/**
 * Admin view for viewing event logs.
 */
@UIScope
@SpringView(name = AdminEventLogSubview.VIEW_NAME)
public class AdminEventLogSubview extends AbstractSubview {

    private final EventLogLayout eventLogLayout;

    public static final String VIEW_NAME = "admin/eventlog";

    @Autowired
    public AdminEventLogSubview(EventLogLayout eventLogLayout) {
        this.eventLogLayout = eventLogLayout;
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
