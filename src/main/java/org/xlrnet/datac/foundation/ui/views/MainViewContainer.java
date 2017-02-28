package org.xlrnet.datac.foundation.ui.views;

import javax.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.foundation.ui.components.NavigationMenu;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;

/**
 * Main view for the application. Represents a container with static navigation menu and main-content subview
 * which is exchanged based on the active view.
 */
@UIScope
@SpringComponent
public class MainViewContainer extends HorizontalLayout {

    private final NavigationMenu navigationMenu;

    private ComponentContainer contentContainer;

    private static final Logger LOGGER = LoggerFactory.getLogger(MainViewContainer.class);

    @Autowired
    public MainViewContainer(@NotNull NavigationMenu navigationMenu) {
        this.navigationMenu = navigationMenu;
    }

    @PostConstruct
    private void init() {
        setSizeFull();
        addStyleName("mainview");
        addComponent(navigationMenu);

        contentContainer = new CssLayout();
        contentContainer.addStyleName("view-content");
        contentContainer.setSizeFull();
        addComponent(contentContainer);
        setExpandRatio(contentContainer, 1.0f);
    }

    /**
     * Displays the given component that is returned by {@link Subview#getContent()} in the main content panel.
     * @param view The subview to display.
     */
    public void displaySubview(@NotNull Subview view) {
        LOGGER.debug("Opening subview for {}", view.getClass());
        contentContainer.removeAllComponents();
        contentContainer.addComponent(view.getContent());
    }
}
