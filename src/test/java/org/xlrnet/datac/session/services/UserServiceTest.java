package org.xlrnet.datac.session.services;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xlrnet.datac.administration.domain.User;
import org.xlrnet.datac.administration.repository.UserRepository;
import org.xlrnet.datac.commons.util.CryptoUtils;
import org.xlrnet.datac.test.util.ReturnFirstArgumentAnswer;

/**
 * Tests for {@link UserService}.
 */
public class UserServiceTest {

    private static final String USER_LOGIN_NAME = "demoUser";

    private static final String TEST_PASSWORD = "Password123";

    private PasswordService passwordService;

    private UserRepository userRepositoryMock;

    private UserService userService;

    @Before
    public void setup() {
        userRepositoryMock = Mockito.mock(UserRepository.class);
        passwordService = Mockito.spy(PasswordService.class);
        userService = new UserService(userRepositoryMock, passwordService);
    }

    @Test
    public void authenticate() throws Exception {
        prepareAuthDataMock();

        Optional<User> authenticatedUser = userService.authenticate(USER_LOGIN_NAME, TEST_PASSWORD);
        assertTrue(authenticatedUser.isPresent());
    }

    @Test
    public void authenticate_wrongPassword() throws Exception {
        prepareAuthDataMock();

        Optional<User> authenticatedUser = userService.authenticate(USER_LOGIN_NAME, "wrongPassword");
        assertFalse(authenticatedUser.isPresent());
    }

    @Test
    public void createNewUser() throws Exception {
        User user = new User();
        user.setLoginName(USER_LOGIN_NAME);
        doReturn(true).when(passwordService).isValid(TEST_PASSWORD);
        when(userRepositoryMock.findFirstByLoginNameIgnoreCase(USER_LOGIN_NAME)).thenReturn(null);
        when(userRepositoryMock.save(any(User.class))).thenAnswer(new ReturnFirstArgumentAnswer());

        Optional<User> createdUser = userService.createNewUser(user, TEST_PASSWORD);

        assertTrue(createdUser.isPresent());
        assertNotNull(createdUser.get().getPassword());
        assertNotNull(createdUser.get().getSalt());
    }

    @Test
    public void createNewUser_invalidPassword() throws Exception {
        User user = new User();
        user.setLoginName(USER_LOGIN_NAME);
        doReturn(false).when(passwordService).isValid(TEST_PASSWORD);
        when(userRepositoryMock.findFirstByLoginNameIgnoreCase(USER_LOGIN_NAME)).thenReturn(null);

        Optional<User> createdUser = userService.createNewUser(user, TEST_PASSWORD);

        assertFalse(createdUser.isPresent());
    }

    @Test
    public void createNewUser_existing() throws Exception {
        User user = new User();
        user.setLoginName(USER_LOGIN_NAME);
        when(userRepositoryMock.findFirstByLoginNameIgnoreCase(USER_LOGIN_NAME)).thenReturn(user);

        Optional<User> createdUser = userService.createNewUser(user, TEST_PASSWORD);

        assertFalse(createdUser.isPresent());
    }

    private void prepareAuthDataMock() {
        byte[] salt = CryptoUtils.generateRandom(CryptoUtils.DEFAULT_SALT_LENGTH);
        byte[] hashedPassword = passwordService.hashPassword(TEST_PASSWORD, salt);

        User user = new User();
        user.setLoginName(USER_LOGIN_NAME);
        user.setPassword(hashedPassword);
        user.setSalt(salt);

        when(userRepositoryMock.findFirstByLoginNameIgnoreCase(USER_LOGIN_NAME)).thenReturn(user);
    }
}