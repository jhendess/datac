package org.xlrnet.datac.foundation.ui.views;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlrnet.datac.administration.ui.views.AdminSubview;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract subview which contains a title with subtitle and a main content panel. Override the abstract methods in this
 * class to build a the user interface.
 */
@UIScope
@SpringView(name = AdminSubview.VIEW_NAME)
public abstract class AbstractSubview extends VerticalLayout implements Subview {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSubview.class);

    @NotNull
    protected abstract Component buildMainPanel();

    protected String parameters;

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        parameters = event.getParameters();
        if (StringUtils.isEmpty(parameters)) {
            LOGGER.debug("Entering subview {}", event.getViewName());
        } else {
            LOGGER.debug("Entering subview {} with parameters {}", event.getViewName(), event.getParameters());
        }

        buildComponents();
    }

    private void buildComponents() {
        Component topPanel = buildTitlePanel();
        Component editPanel = buildMainPanel();

        removeAllComponents();
        addComponent(topPanel);
        addComponent(editPanel);
        setWidth("100%");
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
    @Override
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

    /**
     * Returns the parameters for the current view.
     *
     * @return the parameters for the current view.
     */
    @Nullable
    protected String getParameters() {
        return parameters;
    }
}
