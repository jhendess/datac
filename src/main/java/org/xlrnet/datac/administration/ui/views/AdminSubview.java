package org.xlrnet.datac.administration.ui.views;

import org.jetbrains.annotations.NotNull;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MGridLayout;
import org.xlrnet.datac.administration.ui.views.database.AdminDatabaseSubview;
import org.xlrnet.datac.administration.ui.views.eventlog.AdminEventLogSubview;
import org.xlrnet.datac.administration.ui.views.projects.AdminProjectSubview;
import org.xlrnet.datac.administration.ui.views.user.AdminUserSubview;
import org.xlrnet.datac.session.ui.views.AbstractSubview;
import org.xlrnet.datac.session.ui.views.Subview;

import com.vaadin.server.ThemeResource;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Administration view.
 */
@UIScope
@SpringView(name = AdminSubview.VIEW_NAME)
public class AdminSubview extends AbstractSubview implements Subview {

    public static final String VIEW_NAME = "admin";

    /**
     * CSS class for category buttons.
     */
    private static final String ADMIN_CATEGORY_BUTTON_CLASS = "admin-category-button";

    @Override
    protected void initialize() {
        // Nothing to do
    }

    @NotNull
    @Override
    protected String getTitle() {
        return "System administration";
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        return "Administrate and configure the application. Select below which elements you want to configure.";
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        MGridLayout layout = new MGridLayout(4, 2);

        MButton userButton = new MButton("Users").withIcon(new ThemeResource("img/users-128.png"));
        userButton.withStyleName(ADMIN_CATEGORY_BUTTON_CLASS, "card", "card-2");
        userButton.withStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
        userButton.addClickListener(event -> UI.getCurrent().getNavigator().navigateTo(AdminUserSubview.VIEW_NAME));

        MButton projectButton = new MButton("Projects").withIcon(new ThemeResource("img/project-128.png"));
        projectButton.withStyleName(ADMIN_CATEGORY_BUTTON_CLASS, "card", "card-2");
        projectButton.withStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
        projectButton.addClickListener(event -> UI.getCurrent().getNavigator().navigateTo(AdminProjectSubview.VIEW_NAME));

        MButton databaseButton = new MButton("Databases").withIcon(new ThemeResource("img/database-configuration-128.png"));
        databaseButton.withStyleName(ADMIN_CATEGORY_BUTTON_CLASS, "card", "card-2");
        databaseButton.withStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
        databaseButton.addClickListener(event -> UI.getCurrent().getNavigator().navigateTo(AdminDatabaseSubview.VIEW_NAME));

        MButton eventlogButton = new MButton("Event Log").withIcon(new ThemeResource("img/eventlog-128.png"));
        eventlogButton.withStyleName(ADMIN_CATEGORY_BUTTON_CLASS, "card", "card-2");
        eventlogButton.withStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
        eventlogButton.addClickListener(event -> UI.getCurrent().getNavigator().navigateTo(AdminEventLogSubview.VIEW_NAME));

        layout.addComponent(userButton);
        layout.addComponent(projectButton);
        layout.addComponent(databaseButton);
        layout.addComponent(eventlogButton);

        return layout;
    }
}
