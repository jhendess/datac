package org.xlrnet.datac.session.ui.views;

import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.xlrnet.datac.Application;
import org.xlrnet.datac.administration.services.ApplicationMaintenanceService;

/**
 * Start page after login.
 */
@SpringView(name = HomeView.VIEW_NAME)
public class HomeView extends AbstractSubview implements Subview {

    public static final String VIEW_NAME = "home";

    @Autowired
    public HomeView(EventBus.ApplicationEventBus applicationEventBus, ApplicationMaintenanceService maintenanceService) {
        super(applicationEventBus, maintenanceService);
    }

    @Override
    protected void initialize() {
        // Nothing to do
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        return "Configuration management for databases.";
    }

    @NotNull
    @Override
    protected String getTitle() {
        return "Welcome to " + Application.APPLICATION_NAME + "!";
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        return new VerticalLayout();
    }
}
