package org.xlrnet.datac.foundation.ui.views;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.Application;
import org.xlrnet.datac.foundation.ui.Subview;

import javax.annotation.PostConstruct;

/**
 * Start page after login.
 */
@SpringView(name = HomeView.VIEW_NAME)
public class HomeView extends VerticalLayout implements Subview {

    public static final String VIEW_NAME = "home";

    @NotNull
    @Override
    public Component getContent() {
        return this;
    }

    @PostConstruct
    private void init() {
        Label title = new Label("Welcome to " + Application.APPLICATION_NAME + "!");
        title.setStyleName(ValoTheme.LABEL_H1);
        addComponent(title);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {

    }
}
