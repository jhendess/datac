package org.xlrnet.datac.session.ui.views;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.administration.ui.views.AdminSubview;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.internal.UIScopeImpl;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract subview which contains a titleLabel with subtitleLabel and a main content panel. Override the abstract methods in this
 * class to build a the user interface.
 */
@Scope(UIScopeImpl.VAADIN_UI_SCOPE_NAME)
@SpringView(name = AdminSubview.VIEW_NAME)
public abstract class AbstractSubview extends MVerticalLayout implements Subview {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSubview.class);

    /** Label of the title. */
    private Label titleLabel;

    /** Label of the subtitle. */
    private Label subtitleLabel;

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

        titleLabel = new Label(getTitle());
        titleLabel.setStyleName(ValoTheme.LABEL_H1);
        subtitleLabel = new Label(getSubtitle());

        topPanel.addComponent(titleLabel);
        topPanel.addComponent(subtitleLabel);
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
     * Returns the subtitleLabel of this subview which will be displayed below the titleLabel.
     *
     * @return the subtitleLabel of this subview which will be displayed below the titleLabel.
     */
    @NotNull
    protected abstract String getSubtitle();

    /**
     * Returns the titleLabel of this subview.
     *
     * @return the titleLabel of this subview.
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

    /**
     * Replace the current title with a new one.
     * @param newTitle The new title for this sub view.
     */
    public void updateTitle(@NotNull  String newTitle) {
        this.titleLabel.setValue(newTitle);
    }

    /**
     * Replace the current subtitle with a new one.
     * @param newSubtitle The new subtitle for this sub view.
     */
    public void updateSubtitle(@NotNull String newSubtitle) {
        this.subtitleLabel.setValue(newSubtitle);
    }
}
