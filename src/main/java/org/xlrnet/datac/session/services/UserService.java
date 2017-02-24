package org.xlrnet.datac.session.services;

import com.vaadin.server.VaadinSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.session.SessionAttributes;
import org.xlrnet.datac.session.domain.User;

/**
 * Service used for authentication
 */
@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    /**
     * Tries to authenticate a user with a given username and password. If the authentication succeeds, the currently
     * active {@link com.vaadin.server.VaadinSession} will contain the user information.
     *
     * @param username
     *         Username for authentication.
     * @param password
     *         Password for authentication.
     * @return
     */
    public boolean authenticate(String username, String password) {
        boolean loginSuccessful = "jhendess".equals(username);
        User user = findUserByLoginName(username);      // TODO: Get user only when auth was succesful

        LOGGER.debug("Login attempt for user {} was {}", username, loginSuccessful ? "successful" : "not successful");
        if (loginSuccessful) {
            VaadinSession.getCurrent().setAttribute(SessionAttributes.USERNAME, user);
        }

        return loginSuccessful;
    }

    protected User findUserByLoginName(String username) {
        // TODO: Dummy method - call actual backend
        User user = new User();
        user.setId(1);
        user.setLoginName(username);
        user.setFirstName(username);
        return user;
    }

    public User getSessionUser() {
        return (User) VaadinSession.getCurrent().getAttribute(SessionAttributes.USERNAME);
    }

    public void logout() {
        VaadinSession session = VaadinSession.getCurrent();
        session.setAttribute(SessionAttributes.USERNAME, null);
    }
}
