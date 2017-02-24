package org.xlrnet.datac.session.ui.listener;

import com.vaadin.ui.LoginForm;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.foundation.ui.views.MainView;
import org.xlrnet.datac.session.services.UserService;

/**
 * Listener for processing login attempts.
 */
@Service
public class UserLoginListener implements LoginForm.LoginListener {

    private static final String USERNAME_PARAMETER = "username";

    private static final String PASSWORD_PARAMETER = "password";

    private final UserService userService;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserLoginListener.class);

    @Autowired
    public UserLoginListener(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onLogin(LoginForm.LoginEvent event) {
        String username = event.getLoginParameter(USERNAME_PARAMETER);
        String password = event.getLoginParameter(PASSWORD_PARAMETER);
        boolean loginSuccessful = userService.authenticate(username, password);

        if (loginSuccessful) {
            UI.getCurrent().getNavigator().navigateTo(MainView.VIEW_NAME);
        } else {
            Notification.show("Login failed", Notification.Type.TRAY_NOTIFICATION);
        }
    }
}
