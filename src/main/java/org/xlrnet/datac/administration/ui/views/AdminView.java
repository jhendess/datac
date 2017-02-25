package org.xlrnet.datac.administration.ui.views;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ThemeResource;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.administration.ui.views.user.AdminUserView;
import org.xlrnet.datac.foundation.ui.Subview;

import javax.annotation.PostConstruct;

/**
 * Administration view.
 */
@UIScope
@SpringView(name = AdminView.VIEW_NAME)
public class AdminView extends VerticalLayout implements Subview {

    public static final String VIEW_NAME = "admin";

    /** CSS class for category buttons. */
    private static final String ADMIN_CATEGORY_BUTTON_CLASS = "admin-category-button";

    @NotNull
    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        // No action necessary
    }

    @PostConstruct
    private void init() {
        VerticalLayout topPanel = buildGreetingPanel();
        HorizontalLayout actionPanel = buildActionPanel();
        addComponent(topPanel);
        addComponent(actionPanel);
    }

    @NotNull
    private VerticalLayout buildGreetingPanel() {
        VerticalLayout topPanel = new VerticalLayout();
        topPanel.setSpacing(false);
        topPanel.setMargin(false);

        Label title = new Label("System administration");
        title.setStyleName(ValoTheme.LABEL_H1);
        Label infoText = new Label("Administrate and configure the application. Select below which elements you want to configure.");

        topPanel.addComponent(title);
        topPanel.addComponent(infoText);
        return topPanel;
    }

    @NotNull
    private HorizontalLayout buildActionPanel() {
        HorizontalLayout layout = new HorizontalLayout();

        Button userButton = new Button("Users", new ThemeResource("img/users-128.png"));
        userButton.addStyleName(ADMIN_CATEGORY_BUTTON_CLASS);
        userButton.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
        userButton.addClickListener(event -> UI.getCurrent().getNavigator().navigateTo(AdminUserView.VIEW_NAME));

        Button projectButton = new Button("Projects", new ThemeResource("img/project-128.png"));
        projectButton.addStyleName(ADMIN_CATEGORY_BUTTON_CLASS);
        projectButton.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);

        Button databaseButton = new Button("Databases", new ThemeResource("img/database-configuration-128.png"));
        databaseButton.addStyleName(ADMIN_CATEGORY_BUTTON_CLASS);
        databaseButton.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);


        layout.addComponent(userButton);
        layout.addComponent(projectButton);
        layout.addComponent(databaseButton);

        return layout;
    }
}
