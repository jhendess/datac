package org.xlrnet.datac.administration.ui.views.projects;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.foundation.ui.views.AbstractSubview;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * Admin view for managing projects responsible for managing the available users.
 */
@SpringComponent
@SpringView(name = AdminProjectSubview.VIEW_NAME)
public class AdminProjectSubview extends AbstractSubview {

    public static final String VIEW_NAME = "admin/projects";

    /** Button for new projects. */
    private Button newButton;

    /** Main layout. */
    private VerticalLayout layout;

    @NotNull
    @Override
    protected Component buildMainPanel() {
        layout = new VerticalLayout();

        newButton = new Button("New project");
        newButton.setIcon(VaadinIcons.PLUS);
        newButton.addClickListener((e) -> UI.getCurrent().getNavigator().navigateTo(AdminNewProjectAssistantSubview.VIEW_NAME));
        layout.addComponent(newButton);
        return layout;
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        return "Projects are used to manage a specific database configuration backed by a version control system (VCS).";
    }

    @NotNull
    @Override
    protected String getTitle() {
        return "Project administration";
    }
}
