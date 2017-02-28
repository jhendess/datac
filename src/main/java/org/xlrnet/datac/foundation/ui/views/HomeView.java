package org.xlrnet.datac.foundation.ui.views;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.Application;

import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

/**
 * Start page after login.
 */
@SpringView(name = HomeView.VIEW_NAME)
public class HomeView extends AbstractSubview implements Subview {

    public static final String VIEW_NAME = "home";

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
