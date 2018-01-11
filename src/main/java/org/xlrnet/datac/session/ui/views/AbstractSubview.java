package org.xlrnet.datac.session.ui.views;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.administration.ui.views.AdminSubview;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract subview which contains a title with subtitle and a main content panel. Override the abstract methods in this
 * class to build a the user interface.
 */
@UIScope
@SpringView(name = AdminSubview.VIEW_NAME)
public abstract class AbstractSubview extends MVerticalLayout implements Subview {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSubview.class);

    @NotNull
    protected abstract Component buildMainPanel();

    /**
     * Perform custom initialization logic. Will be called after {@link #enter(ViewChangeListener.ViewChangeEvent)}} and
     * before any components are built.
     */
    protected abstract void initialize() throws DatacTechnicalException;

    /**
     * Map of named parameters. I.e. everything after "?" in /xxx/yyy?a=b
     */
    private Map<String, String> namedParameters;

    /**
     * Array of parameters after the current view's name except named parameters.
     */
    private String[] parameters;

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        String namedParametersRaw = StringUtils.substringAfter(event.getParameters(), "?");
        parameters = StringUtils.split(StringUtils.substringBefore(event.getParameters(), "?"), "/");
        namedParameters = buildNamedParameterMap(namedParametersRaw);
        if (parameters.length > 0 || !namedParameters.isEmpty()) {
            LOGGER.debug("Entering subview {} with parameters {} and {}", event.getViewName(), parameters, namedParameters);
        } else {
            LOGGER.debug("Entering subview {}", event.getViewName());
        }

        try {
            initialize();
            buildComponents();
        } catch (DatacTechnicalException | DatacRuntimeException e) {
            LOGGER.error("Initializing view {} failed", event.getViewName(), e);
            UI.getCurrent().getNavigator().navigateTo(HomeView.VIEW_NAME);
        }
    }

    private Map<String, String> buildNamedParameterMap(String namedParametersRaw) {
        Map<String, String> namedParameterMap = new HashMap<>();
        String[] kvPairs = StringUtils.split(namedParametersRaw, "&");
        for (String kvPair : kvPairs) {
            String[] split = StringUtils.split(kvPair, "=");
            namedParameterMap.put(split[0], split[1]);
        }
        return namedParameterMap;
    }

    /**
     * Returns the given named parameter or the given default value if it is null.
     *
     * @param parameterName
     *         The name of the parameter look up.
     * @param defaultValue
     *         The default value to use if the value of the given parameter is null.
     * @return
     */
    String getNamedParameter(String parameterName, String defaultValue) {
        return namedParameters.getOrDefault(parameterName, defaultValue);
    }

    /**
     * Returns the given named parameter or null if it doesn't exist.
     */
    String getNamedParameter(String branchParameter) {
        return getNamedParameter(branchParameter, null);
    }

    private void buildComponents() {
        Component topPanel = buildTitlePanel();
        Component editPanel = buildMainPanel();

        removeAllComponents();
        addComponent(topPanel);
        addComponent(editPanel);
    }

    @NotNull
    protected Component buildTitlePanel() {
        MVerticalLayout topPanel = new MVerticalLayout()
                .withFullSize()
                .withMargin(false)
                .withSpacing(false);

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
     * Returns the parameters for the current view. If no additional parameters are present, the returned array is empty.
     *
     * @return the parameters for the current view.
     */
    @NotNull
    protected String[] getParameters() {
        return parameters;
    }
}
