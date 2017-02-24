package org.xlrnet.datac.session.ui.views;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.Application;
import org.xlrnet.datac.foundation.BuildInformation;
import org.xlrnet.datac.session.ui.components.CustomLoginForm;
import org.xlrnet.datac.session.ui.listener.UserLoginListener;

import javax.annotation.PostConstruct;

/**
 * Login view for the application. Based on https://github.com/vaadin/dashboard-demo/blob/7.7/src/main/java/com/vaadin/demo/dashboard/view/LoginView.java
 */
@UIScope
@SpringView(name = LoginView.VIEW_NAME)
public class LoginView extends HorizontalLayout implements View {

    private final LoginForm.LoginListener loginListener;

    public static final String VIEW_NAME = "login";

    private final BuildInformation buildInformation;

    @Autowired
    public LoginView(UserLoginListener loginListener, BuildInformation buildInformation) {
        this.loginListener = loginListener;
        this.buildInformation = buildInformation;
    }

    @PostConstruct
    void init() {
        setSizeFull();

        Component loginForm = buildLoginPanel();
        addComponent(loginForm);
        setComponentAlignment(loginForm, Alignment.MIDDLE_CENTER);
    }

    private Component buildLoginPanel() {
        final HorizontalLayout loginPanel = new HorizontalLayout();
        Responsive.makeResponsive(loginPanel);
        loginPanel.setSizeUndefined();
        loginPanel.setSpacing(true);
        loginPanel.addStyleName("login-panel");

        VerticalLayout loginInputPanel = new VerticalLayout();
        loginInputPanel.addComponent(buildLabels());
        loginInputPanel.addComponent(buildFields());

        Image logo = new Image(null, new ThemeResource("img/database-128.png"));
        loginPanel.addComponent(logo);
        loginPanel.addComponent(loginInputPanel);
        loginPanel.setComponentAlignment(logo, Alignment.MIDDLE_LEFT);
        loginPanel.setComponentAlignment(loginInputPanel, Alignment.MIDDLE_RIGHT);

        return loginPanel;
    }

    private Component buildFields() {
        CustomLoginForm loginForm = new CustomLoginForm();
        loginForm.addLoginListener(loginListener);
        return loginForm;
    }

    private Component buildLabels() {
        CssLayout labels = new CssLayout();
        labels.addStyleName("labels");

        Label welcome = new Label(Application.APPLICATION_NAME);
        welcome.setSizeUndefined();
        welcome.addStyleName(ValoTheme.LABEL_H2);
        welcome.addStyleName(ValoTheme.LABEL_COLORED);
        labels.addComponent(welcome);

        Label title = new Label(this.buildInformation.getVersion());
        title.setSizeUndefined();
        title.addStyleName(ValoTheme.LABEL_H3);
        title.addStyleName(ValoTheme.LABEL_LIGHT);

        labels.addComponent(title);

        return labels;
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        // This view is constructed in the init() method()
    }
}
