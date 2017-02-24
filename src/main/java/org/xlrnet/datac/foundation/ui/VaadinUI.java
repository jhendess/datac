package org.xlrnet.datac.foundation.ui;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.SpringViewDisplay;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.Application;
import org.xlrnet.datac.foundation.ui.views.MainView;
import org.xlrnet.datac.session.ui.listener.SessionCheckViewChangeListener;

@SpringUI
@Title(Application.APPLICATION_NAME)
@Theme(VaadinUI.THEME_NAME)
@SpringViewDisplay
public class VaadinUI extends UI implements ViewDisplay {

    static final String THEME_NAME = "mytheme";

    @Autowired
    public VaadinUI() {
    }

    @Override
    protected void init(VaadinRequest request) {
        Responsive.makeResponsive(this);

        getNavigator().addViewChangeListener(new SessionCheckViewChangeListener());
        getNavigator().navigateTo(MainView.VIEW_NAME);
    }

    @Override
    public void showView(View view) {
        setContent((Component) view);
    }
}