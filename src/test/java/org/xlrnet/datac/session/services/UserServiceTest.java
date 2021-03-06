package org.xlrnet.datac.session.services;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xlrnet.datac.commons.util.CryptoUtils;
import org.xlrnet.datac.session.domain.User;
import org.xlrnet.datac.session.domain.repository.UserRepository;
import org.xlrnet.datac.test.util.ReturnFirstArgumentAnswer;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link UserService}.
 */
public class UserServiceTest {

    private static final String USER_LOGIN_NAME = "demoUser";

    private static final String TEST_PASSWORD = "Password123";

    private CryptoService cryptoService;

    private UserRepository userRepositoryMock;

    private UserService userService;

    @Before
    public void setup() {
        userRepositoryMock = Mockito.mock(UserRepository.class);
        cryptoService = Mockito.spy(CryptoService.class);
        userService = new UserService(userRepositoryMock, cryptoService);
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
        doReturn(true).when(cryptoService).isValid(TEST_PASSWORD);
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
        doReturn(false).when(cryptoService).isValid(TEST_PASSWORD);
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
        byte[] salt = cryptoService.generateSafeRandom(CryptoUtils.DEFAULT_SALT_LENGTH);
        byte[] hashedPassword = cryptoService.hashPassword(TEST_PASSWORD, salt);

        User user = new User();
        user.setLoginName(USER_LOGIN_NAME);
        user.setPassword(hashedPassword);
        user.setSalt(salt);

        when(userRepositoryMock.findFirstByLoginNameIgnoreCase(USER_LOGIN_NAME)).thenReturn(user);
    }
}