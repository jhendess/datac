package org.xlrnet.datac.administration.ui.views;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;

/**
 * Administration view.
 */
@UIScope
@SpringView(name = AdminView.VIEW_NAME)
public class AdminView implements View {

    public static final String VIEW_NAME = "admin";

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {

    }
}
