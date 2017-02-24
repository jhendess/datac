package org.xlrnet.datac.foundation.ui;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.server.Resource;
import org.xlrnet.datac.administration.ui.views.AdminView;
import org.xlrnet.datac.foundation.ui.views.MainView;

public enum ViewType {
    HOME(MainView.VIEW_NAME, "Home", MainView.class, VaadinIcons.HOME),

    ADMINSTRATION(AdminView.VIEW_NAME, "Adminstration", AdminView.class, VaadinIcons.COGS);

    private final String displayString;

    private final String viewName;

    private final Class<? extends View> viewClass;

    private final Resource icon;

    ViewType(String viewName, final String displayString,
             final Class<? extends View> viewClass, final Resource icon) {
        this.viewName = viewName;
        this.displayString = displayString;
        this.viewClass = viewClass;
        this.icon = icon;
    }

    public String getDisplayString() {
        return displayString;
    }

    public Class<? extends View> getViewClass() {
        return viewClass;
    }

    public Resource getIcon() {
        return icon;
    }

    public String getViewName() {
        return viewName;
    }

    public static ViewType getByViewName(final String viewName) {
        ViewType result = null;
        for (ViewType viewType : values()) {
            if (viewType.getDisplayString().equals(viewName)) {
                result = viewType;
                break;
            }
        }
        return result;
    }

}
