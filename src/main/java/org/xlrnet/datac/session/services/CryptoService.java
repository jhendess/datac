package org.xlrnet.datac.session.services;

import org.apache.commons.lang3.RandomStringUtils;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.salt.ByteArrayFixedSaltGenerator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xlrnet.datac.commons.exception.EncryptionFailedException;
import org.xlrnet.datac.commons.util.CryptoUtils;
import org.xlrnet.datac.session.domain.User;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Service for generating and validating passwords. Passwords must match at least three of the following
 * properties:
 * <ul>
 * <li>At least one uppercase character</li>
 * <li>At least one lowercase character</li>
 * <li>At least one special character</li>
 * <li>At least one digit</li>
 * </ul>
 */
@Service
public class CryptoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoService.class);

    /**
     * Random generator.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String DEFAULT_SECRET_KEY = "DEFAULT_SECRET_KEY";

    @Value("${datac.secretKey}")
    private transient String secretKey;

    /**
     * Minimum password size.
     */
    public static final int MINIMUM_PASSWORD_SIZE = 6;

    /**
     * Maximum password size.
     */
    public static final int MAXIMUM_PASSWORD_SIZE = 32;

    /**
     * Number of rules that need to match for a valid password.
     */
    private static final int REQUIRED_RULE_MATCHES = 3;

    /**
     * Contains uppercase character.
     */
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");

    /**
     * Contains uppercase lowercase.
     */
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");

    /**
     * Contains uppercase special char.
     */
    private static final Pattern HAS_SPECIALCHAR = Pattern.compile("[^a-zA-Z0-9 ]");

    /**
     * Contains uppercase number.
     */
    private static final Pattern HAS_NUMBER = Pattern.compile("\\d");

    /**
     * Uppercase characters for password generation.
     */
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Lowercase characters for password generation.
     */
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";

    /**
     * Numbers for password generation.
     */
    private static final String NUMBERS = "0123456789";

    /**
     * Encrypt a given clear-text string using the given salt and the internally configured application secret. This
     * will produce a decryptable string. <b>NO</b> hashing will be used! For non-recoverable passwords, consider using
     * {@link #hashPassword(String, byte[])}.
     *
     * @param string
     *         The string to encrypt.
     * @param salt
     *         The salt used for encryption.
     * @return An encrypted string which can be restored.
     */
    public String encryptString(String string, byte[] salt) {
        try {
            LOGGER.trace("Encrypting string");
            StandardPBEStringEncryptor stringEncryptor = getEncryptor(salt);
            return stringEncryptor.encrypt(string);
        } catch (EncryptionOperationNotPossibleException e) {
            LOGGER.error("Encrypting string value failed", e);
            throw new EncryptionFailedException(e);
        }
    }

    /**
     * Returns an initializes string encryptor using the given salt and the configured application secret.
     */
    @NotNull
    private StandardPBEStringEncryptor getEncryptor(byte[] salt) {
        StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();
        stringEncryptor.setSaltGenerator(new ByteArrayFixedSaltGenerator(salt));
        stringEncryptor.setPassword(getSecretKey());
        stringEncryptor.initialize();
        return stringEncryptor;
    }

    /**
     * Decrypt a given string using the given salt and the internally configured application secret. The method will
     * always decrypt the given input - even if its decrypted form is wrong.
     *
     * @param encryptedString
     *         The string to decrypt.
     * @param salt
     *         The salt used for encryption.
     * @return A clear-text representation of the given string.
     */
    public String decryptString(String encryptedString, byte[] salt) {
        try {
            LOGGER.trace("Decrypting string");
            StandardPBEStringEncryptor encryptor = getEncryptor(salt);
            return encryptor.decrypt(encryptedString);
        } catch (EncryptionOperationNotPossibleException e) {
            LOGGER.error("Decrypting string value failed - make sure that you use the same secret key as for encoding", e);
            throw new EncryptionFailedException(e);
        }
    }

    /**
     * Checks if the given password is valid. Passwords must match at least three of the following
     * properties to be valid:
     * <ul>
     * <li>At least one uppercase character</li>
     * <li>At least one lowercase character</li>
     * <li>At least one special character</li>
     * <li>At least one digit</li>
     * </ul>
     *
     * @param password
     *         The password to check
     * @return True if valid, otherwise false.
     */
    public boolean isValid(@NotNull String password) {
        int matches = 0;

        matches += HAS_UPPERCASE.matcher(password).find() ? 1 : 0;
        matches += HAS_LOWERCASE.matcher(password).find() ? 1 : 0;
        matches += HAS_SPECIALCHAR.matcher(password).find() ? 1 : 0;
        matches += HAS_NUMBER.matcher(password).find() ? 1 : 0;

        boolean hasEnoughMatches = matches >= REQUIRED_RULE_MATCHES;
        return hasEnoughMatches && !StringUtils.containsWhitespace(password);
    }

    /**
     * Generates a random password with a given length.
     *
     * @param length
     *         Length of the new password. Must be greater than 0.
     * @return a generated random password.
     */
    public String generateUserPassword(int length) {
        checkArgument(length > 0, "Password length must be greater than 0");

        StringBuilder stringBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            switch (i % 3) {        // NOSONAR: there is no default other branch possible
                case 0:
                    stringBuilder.append(RandomStringUtils.random(1, LOWERCASE));
                    break;
                case 1:
                    stringBuilder.append(RandomStringUtils.random(1, UPPERCASE));
                    break;
                case 2:
                    stringBuilder.append(RandomStringUtils.random(1, NUMBERS));
                    break;
            }
        }

        return stringBuilder.toString();
    }

    /**
     * Checks if the given unhashed password matches the hashed password in the given user object.
     *
     * @param user
     *         The user to check.
     * @param password
     *         The password for authentication.
     * @return True if the password is valid, otherwise false.
     */
    public boolean checkPassword(User user, String password) {
        byte[] newHash = hashPassword(password, user.getSalt());
        return Arrays.equals(user.getPassword(), newHash);
    }

    /**
     * Changes the password in the given user object to the given and reset the change-necessary status. This method
     * does <strong>not</strong> perform any backend operation.
     *
     * @param sessionUser
     *         The user to update.
     * @param newPassword
     *         The new password to set.
     */
    public void changePassword(User sessionUser, String newPassword) {
        byte[] hashedPassword = hashPassword(newPassword, sessionUser.getSalt());
        sessionUser.setPassword(hashedPassword);
        sessionUser.setPwChangeNecessary(false);
    }

    /**
     * Hashes a given string using a given salt.
     *
     * @param unhashedPassword
     *         The unhashed password to hash.
     * @param salt
     *         The salt.
     * @return Binary representation of the hashed password.
     */
    @NotNull
    public byte[] hashPassword(@NotNull String unhashedPassword, @NotNull byte[] salt) {
        return CryptoUtils.hashPassword(unhashedPassword.toCharArray(), salt, CryptoUtils.DEFAULT_ITERATIONS, CryptoUtils.DEFAULT_KEYLENGTH);
    }

    /**
     * Generates a safe random of the given length..
     *
     * @param length
     *         Length of the new salt.
     * @return A base64 encoded salt.
     */
    public byte[] generateSafeRandom(int length) {
        byte[] salt = new byte[length];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    @NotNull
    private String getSecretKey() {
        // TODO: This null check and alternative value is only used because the test profile doesn't detect the actual key
        return secretKey != null ? secretKey : DEFAULT_SECRET_KEY;
    }
}
