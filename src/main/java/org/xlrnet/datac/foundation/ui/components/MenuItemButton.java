package org.xlrnet.datac.foundation.ui.components;

import com.vaadin.ui.Button;
import com.vaadin.ui.UI;

public final class MenuItemButton extends Button {

    private final MainMenuEntry view;

    public MenuItemButton(final MainMenuEntry view) {
        this.view = view;
        setPrimaryStyleName("valo-menu-item");
        setIcon(view.getIcon());
        setCaption(view.getDisplayString());
        addClickListener((ClickListener) event -> UI.getCurrent().getNavigator().navigateTo(view.getViewName()));
    }
}