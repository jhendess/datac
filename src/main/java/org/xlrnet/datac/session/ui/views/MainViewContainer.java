package org.xlrnet.datac.session.ui.views;

import javax.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.layouts.MCssLayout;
import org.xlrnet.datac.foundation.ui.components.NavigationMenu;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.ComponentContainer;

/**
 * Main view for the application. Represents a container with static navigation menu and main-content subview
 * which is exchanged based on the active view.
 */
@UIScope
@SpringComponent
public class MainViewContainer extends MCssLayout {

    private final NavigationMenu navigationMenu;

    private ComponentContainer contentView;

    private static final Logger LOGGER = LoggerFactory.getLogger(MainViewContainer.class);

    @Autowired
    public MainViewContainer(@NotNull NavigationMenu navigationMenu) {
        this.navigationMenu = navigationMenu;
    }

    @PostConstruct
    private void init() {
        withFullWidth();

        addStyleName("main-container");
        addComponent(navigationMenu);

        MCssLayout contentContainer = new MCssLayout().withStyleName("view-container");
        addComponent(contentContainer);
        contentView = new MCssLayout().withStyleName("view-content", "card", "card-1");
        contentContainer.add(contentView);
    }

    /**
     * Displays the given component that is returned by {@link Subview#getContent()} in the main content panel.
     *
     * @param view
     *         The subview to display.
     */
    public void displaySubview(@NotNull Subview view) {
        LOGGER.debug("Opening subview for {}", view.getClass());
        contentView.removeAllComponents();
        contentView.addComponent(view.getContent());
    }
}
