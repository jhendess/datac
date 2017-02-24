package org.xlrnet.datac.foundation.ui.views;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.foundation.ui.components.NavigationMenu;

/**
 * Main view for the application
 */
@UIScope
@SpringView(name = MainView.VIEW_NAME)
public class MainView extends HorizontalLayout implements View {

    public static final String VIEW_NAME = "main";

    private final NavigationMenu navigationMenu;

    @Autowired
    public MainView(NavigationMenu navigationMenu) {
        this.navigationMenu = navigationMenu;
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent viewChangeEvent) {
        setSizeFull();
        addStyleName("mainview");
        addComponent(navigationMenu);

        ComponentContainer content = new CssLayout();
        content.addStyleName("view-content");
        content.setSizeFull();
        addComponent(content);
        setExpandRatio(content, 1.0f);
    }
}
