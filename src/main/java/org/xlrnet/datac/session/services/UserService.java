package org.xlrnet.datac.session.services;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.administration.domain.User;
import org.xlrnet.datac.administration.repository.UserRepository;
import org.xlrnet.datac.commons.util.CryptoUtils;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.session.SessionAttributes;

import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

/**
 * Service used for authenticating and managing users.
 */
@Service
public class UserService extends AbstractTransactionalService<User, UserRepository> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final PasswordService passwordService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordService passwordService) {
        super(userRepository);
        this.passwordService = passwordService;
    }

    /**
     * Tries to authenticate a user with a given username and password. If authentication was successful, an optional
     * containing the user will be returned. Otherwise the optional will be empty.
     *
     * @param loginName
     *         Username for authentication.
     * @param password
     *         Password for authentication.
     * @return authenticated user or null if authentication failed.
     */
    @NotNull
    public Optional<User> authenticate(@NotNull String loginName, @NotNull String password) {
        User user = getRepository().findFirstByLoginNameIgnoreCase(loginName);

        if (user != null) {
            boolean loginSuccessful = passwordService.checkPassword(user, password);

            LOGGER.debug("Login attempt for user {} was {}", loginName, loginSuccessful ? "successful" : "not successful");
            if (loginSuccessful) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    /**
     * Creates a new user in the database and calculates both a new salt and hashed password. If the user already
     * exists or if the password doesn't meet the complexity requirements, an empty optional will be returned.
     *
     * @param user
     *         The user to persist.
     * @param unhashedPassword
     *         An unhashed password that will be used for authentication.
     * @return the new user in the database or null if a user with the same login name already exists.
     */
    @NotNull
    @Transactional
    public Optional<User> createNewUser(@NotNull User user, @NotNull String unhashedPassword) {
        User existingUser = getRepository().findFirstByLoginNameIgnoreCase(user.getLoginName());
        if (existingUser == null && passwordService.isValid(unhashedPassword)) {
            byte[] salt = CryptoUtils.generateRandom(CryptoUtils.DEFAULT_SALT_LENGTH);
            byte[] hashedPassword = passwordService.hashPassword(unhashedPassword, salt);

            user.setPassword(hashedPassword);
            user.setSalt(salt);

            User save = getRepository().save(user);

            if (save != null) {
                LOGGER.debug("Created new user {}", user.getLoginName());
            } else {
                LOGGER.error("Creating new user {} failed", user.getLoginName());
            }
            return Optional.ofNullable(save);
        } else {
            LOGGER.error("User creation failed for user {}", user.getLoginName());
            return Optional.empty();
        }
    }

    public User getSessionUser() {
        return (User) VaadinSession.getCurrent().getAttribute(SessionAttributes.USERNAME);
    }

    public void logout() {
        VaadinSession session = VaadinSession.getCurrent();
        session.setAttribute(SessionAttributes.USERNAME, null);

        closeSession(session);
    }

    private void closeSession(VaadinSession session) {
        session.close();
        session.getService().closeSession(session);
        UI.getCurrent().close();
        UI.getCurrent().getPage().setLocation(VaadinServlet.getCurrent().getServletContext().getContextPath());
    }
}
