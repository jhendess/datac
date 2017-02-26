package org.xlrnet.datac.session.services;

import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;
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

import java.util.Arrays;
import java.util.Optional;

/**
 * Service used for authenticating and managing users.
 */
@Service
public class UserService extends AbstractTransactionalService<User, UserRepository> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Autowired
    public UserService(UserRepository userRepository) {
        super(userRepository);
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
            boolean loginSuccessful = checkPassword(user, password);

            LOGGER.debug("Login attempt for user {} was {}", loginName, loginSuccessful ? "successful" : "not successful");
            if (loginSuccessful) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    /**
     * Creates a new user in the database and calculates both a new salt and hashed password. If the user already
     * exists, an empty optional will be returned.
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
        if (existingUser == null) {
            // TODO: Validate password
            byte[] salt = CryptoUtils.generateRandom(CryptoUtils.DEFAULT_SALT_LENGTH);
            byte[] hashedPassword = hashPassword(unhashedPassword, salt);

            user.setPassword(hashedPassword);
            user.setSalt(salt);

            return Optional.of(getRepository().save(user));
        } else {
            LOGGER.warn("User creation failed: user {} already exists", existingUser.getLoginName());
            return Optional.empty();
        }
    }

    @NotNull
    byte[] hashPassword(@NotNull String unhashedPassword, @NotNull byte[] salt) {
        return CryptoUtils.hashPassword(unhashedPassword.toCharArray(), salt, CryptoUtils.DEFAULT_ITERATIONS, CryptoUtils.DEFAULT_KEYLENGTH);
    }

    private boolean checkPassword(User user, String password) {
        byte[] newHash = hashPassword(password, user.getSalt());
        return Arrays.equals(user.getPassword(), newHash);
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
