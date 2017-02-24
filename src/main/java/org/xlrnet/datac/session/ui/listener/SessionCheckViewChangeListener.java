package org.xlrnet.datac.session.ui.listener;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlrnet.datac.session.ui.views.LoginView;

/**
 * ViewChangeListener which checks if a user is currently logged in. If no user is logged in, he will be redirected to
 * the login page.
 */
public class SessionCheckViewChangeListener implements ViewChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionCheckViewChangeListener.class);

    @Override
    public boolean beforeViewChange(ViewChangeEvent event) {

        // Check if a user has logged in
        boolean isLoggedIn = UI.getCurrent().getSession().getAttribute("user") != null;
        boolean isLoginView = event.getNewView() instanceof LoginView;

        if (!isLoggedIn && !isLoginView) {
            // Redirect to login view if a user has not yet logged in
            UI.getCurrent().getNavigator().navigateTo(LoginView.VIEW_NAME);
            return false;

        } else if (isLoggedIn && isLoginView) {
            // If someone tries to access to login view while logged in then cancel
            return false;
        }

        return true;
    }
}
