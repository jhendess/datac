package org.xlrnet.datac.foundation.ui.components;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Resource;
import org.xlrnet.datac.administration.ui.views.AdminSubview;
import org.xlrnet.datac.session.ui.views.HomeView;
import org.xlrnet.datac.session.ui.views.ProjectOverviewSubview;

public enum MainMenuEntry {
    HOME(HomeView.VIEW_NAME, "Home", VaadinIcons.HOME),

    PROJECTS(ProjectOverviewSubview.VIEW_NAME, "Projects", VaadinIcons.CUBE),

    ADMINSTRATION(AdminSubview.VIEW_NAME, "Adminstration", VaadinIcons.COGS);

    private final String displayString;

    private final String viewName;

    private final Resource icon;

    MainMenuEntry(String viewName, final String displayString, final Resource icon) {
        this.viewName = viewName;
        this.displayString = displayString;
        this.icon = icon;
    }

    public String getDisplayString() {
        return displayString;
    }

    public Resource getIcon() {
        return icon;
    }

    public String getViewName() {
        return viewName;
    }

    public static MainMenuEntry getByViewName(final String viewName) {
        MainMenuEntry result = null;
        for (MainMenuEntry mainMenuEntry : values()) {
            if (mainMenuEntry.getDisplayString().equals(viewName)) {
                result = mainMenuEntry;
                break;
            }
        }
        return result;
    }
}
