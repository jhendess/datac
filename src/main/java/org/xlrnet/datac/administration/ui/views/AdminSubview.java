package org.xlrnet.datac.administration.ui.views;

import com.vaadin.server.ThemeResource;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;
import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.administration.ui.views.projects.AdminProjectSubview;
import org.xlrnet.datac.administration.ui.views.user.AdminUserSubview;
import org.xlrnet.datac.commons.ui.NotificationUtils;
import org.xlrnet.datac.foundation.ui.views.AbstractSubview;
import org.xlrnet.datac.foundation.ui.views.Subview;

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
        HorizontalLayout layout = new HorizontalLayout();

        Button userButton = new Button("Users", new ThemeResource("img/users-128.png"));
        userButton.addStyleName(ADMIN_CATEGORY_BUTTON_CLASS);
        userButton.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
        userButton.addClickListener(event -> UI.getCurrent().getNavigator().navigateTo(AdminUserSubview.VIEW_NAME));

        Button projectButton = new Button("Projects", new ThemeResource("img/project-128.png"));
        projectButton.addStyleName(ADMIN_CATEGORY_BUTTON_CLASS);
        projectButton.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
        projectButton.addClickListener(event -> UI.getCurrent().getNavigator().navigateTo(AdminProjectSubview.VIEW_NAME));

        Button databaseButton = new Button("Databases", new ThemeResource("img/database-configuration-128.png"));
        databaseButton.addStyleName(ADMIN_CATEGORY_BUTTON_CLASS);
        databaseButton.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
        databaseButton.addClickListener(event -> NotificationUtils.showNotImplemented());


        layout.addComponent(userButton);
        layout.addComponent(projectButton);
        layout.addComponent(databaseButton);

        return layout;
    }
}
