package org.xlrnet.datac.session.services;

import static com.google.common.base.Preconditions.checkArgument;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xlrnet.datac.administration.domain.User;
import org.xlrnet.datac.commons.util.CryptoUtils;

/**
 * Service for generating and validating passwords. Passwords must match at least three of the following
 * properties:
 * <ul>
 *     <li>At least one uppercase character</li>
 *     <li>At least one lowercase character</li>
 *     <li>At least one special character</li>
 *     <li>At least one digit</li>
 * </ul>
 */
@Service
public class PasswordService {
    /** Minimum password size. */
    public static final int MINIMUM_PASSWORD_SIZE = 6;

    /** Maximum password size. */
    public static final int MAXIMUM_PASSWORD_SIZE = 32;

    /** Number of rules that need to match for a valid password. */
    private static final int REQUIRED_RULE_MATCHES = 3;

    /** Contains uppercase character. */
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");

    /** Contains uppercase lowercase. */
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");

    /** Contains uppercase special char. */
    private static final Pattern HAS_SPECIALCHAR = Pattern.compile("[^a-zA-Z0-9 ]");

    /** Contains uppercase number. */
    private static final Pattern HAS_NUMBER = Pattern.compile("\\d");

    /** Uppercase characters for password generation. */
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** Lowercase characters for password generation. */
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";

    /** Numbers for password generation. */
    private static final String NUMBERS = "0123456789";

    /** Secure random for password generation. */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Checks if the given password is valid. Passwords must match at least three of the following
     * properties to be valid:
     * <ul>
     *     <li>At least one uppercase character</li>
     *     <li>At least one lowercase character</li>
     *     <li>At least one special character</li>
     *     <li>At least one digit</li>
     * </ul>
     * @param password The password to check
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
     * @param length Length of the new password. Must be greater than 0.
     * @return a generated random password.
     */
    public String generatePassword(int length) {
        checkArgument(length > 0, "Password length must be greater than 0");

        StringBuilder stringBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            switch (i % 3) {
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
     * @param user      The user to check.
     * @param password   The password for authentication.
     * @return True if the password is valid, otherwise false.
     */
    public boolean checkPassword(User user, String password) {
        byte[] newHash = hashPassword(password, user.getSalt());
        return Arrays.equals(user.getPassword(), newHash);
    }

    /**
     * Hashes a given string using a given salt.
     * @param unhashedPassword The unhashed password to hash.
     * @param salt             The salt.
     * @return Binary representation of the hashed password.
     */
    @NotNull
    public byte[] hashPassword(@NotNull String unhashedPassword, @NotNull byte[] salt) {
        return CryptoUtils.hashPassword(unhashedPassword.toCharArray(), salt, CryptoUtils.DEFAULT_ITERATIONS, CryptoUtils.DEFAULT_KEYLENGTH);
    }
}
