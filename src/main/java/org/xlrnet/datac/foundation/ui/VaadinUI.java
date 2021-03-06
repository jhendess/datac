package org.xlrnet.datac.foundation.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.Application;
import org.xlrnet.datac.commons.util.LoggingVaadinErrorHandler;
import org.xlrnet.datac.session.ui.listener.SessionCheckViewChangeListener;
import org.xlrnet.datac.session.ui.views.HomeView;
import org.xlrnet.datac.session.ui.views.MainViewContainer;
import org.xlrnet.datac.session.ui.views.Subview;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.SpringViewDisplay;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

@Push
@SpringUI
@Title(Application.APPLICATION_NAME)
@Theme(VaadinUI.THEME_NAME)
@SpringViewDisplay
public class VaadinUI extends UI implements ViewDisplay {

    static final String THEME_NAME = "datac";

    private final MainViewContainer mainView;

    /** Error handler which logs to the event log and displays a popup box to the user. */
    private final LoggingVaadinErrorHandler errorHandler;

    @Autowired
    public VaadinUI(MainViewContainer mainView, LoggingVaadinErrorHandler errorHandler) {
        this.mainView = mainView;
        this.errorHandler = errorHandler;
    }

    @Override
    protected void init(VaadinRequest request) {
        setErrorHandler(errorHandler);
        getNavigator().addViewChangeListener(new SessionCheckViewChangeListener());
        getNavigator().navigateTo(HomeView.VIEW_NAME);
    }

    /**
     * This is the central dispatcher for displaying views. If the view is a {@link Subview}, it will be displayed as part of the main view container.
     *
     * @param view
     *         The view to display
     */
    @Override
    public void showView(View view) {
        if (view instanceof Subview) {
            mainView.displaySubview((Subview) view);
            setContent(mainView);
        } else {
            setContent((Component) view);
        }
    }
}