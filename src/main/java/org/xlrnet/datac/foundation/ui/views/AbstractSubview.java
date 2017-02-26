package org.xlrnet.datac.foundation.ui.views;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.foundation.ui.Subview;

import javax.annotation.PostConstruct;

/**
 * Abstract subview which contains a title with subtitle and a main content panel. Override the abstract methods in this
 * class to build a the user interface.
 */
public abstract class AbstractSubview extends VerticalLayout implements Subview {

    @NotNull
    protected abstract Component buildMainPanel();

    @PostConstruct
    private void init() {
        Component topPanel = buildTitlePanel();
        Component editPanel = buildMainPanel();

        addComponent(topPanel);
        addComponent(editPanel);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        // No action necessary per default
    }

    @NotNull
    protected Component buildTitlePanel() {
        VerticalLayout topPanel = new VerticalLayout();
        topPanel.setSpacing(false);
        topPanel.setMargin(false);

        Label title = new Label(getTitle());
        title.setStyleName(ValoTheme.LABEL_H1);
        Label infoText = new Label(getSubtitle());

        topPanel.addComponent(title);
        topPanel.addComponent(infoText);
        return topPanel;
    }

    /**
     * Returns this.
     *
     * @return this.
     */
    @NotNull
    public Component getContent() {
        return this;
    }

    /**
     * Returns the subtitle of this subview which will be displayed below the title.
     *
     * @return the subtitle of this subview which will be displayed below the title.
     */
    @NotNull
    protected abstract String getSubtitle();

    /**
     * Returns the title of this subview.
     *
     * @return the title of this subview.
     */
    @NotNull
    protected abstract String getTitle();
}
