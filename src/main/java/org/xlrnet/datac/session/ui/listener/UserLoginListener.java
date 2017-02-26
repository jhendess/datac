package org.xlrnet.datac.session.ui.listener;

import com.vaadin.server.Page;
import com.vaadin.shared.Position;
import com.vaadin.ui.LoginForm;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.foundation.ui.ViewType;
import org.xlrnet.datac.session.services.UserService;

/**
 * Listener for processing login attempts.
 */
@Service
public class UserLoginListener implements LoginForm.LoginListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserLoginListener.class);

    private static final String USERNAME_PARAMETER = "username";

    private static final String PASSWORD_PARAMETER = "password";        // NOSONAR: Just a key for password parameter

    private final UserService userService;

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
            UI.getCurrent().getNavigator().navigateTo(ViewType.HOME.getViewName());
        } else {
            LOGGER.warn("Login for user {} failed", username);
            Notification error = new Notification("Error", "User login failed", Notification.Type.ERROR_MESSAGE);
            error.setPosition(Position.BOTTOM_RIGHT);
            error.show(Page.getCurrent());
            error.setDelayMsec(5000);
        }
    }
}
