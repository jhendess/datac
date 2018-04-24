package org.xlrnet.datac.foundation.domain;

/**
 * Interface for entities which contain an encrypted password and salt.
 */
public interface PasswordEncryptedEntity {

    /**
     * @return the salt used for encryption.
     */
    byte[] getSalt();

    /**
     * Sets the salt used for encryption.
     * @param salt The salt.
     */
    void setSalt(byte[] salt);

    /**
     * Returns the encrypted representation of the password encoded as base64.
     * @return the encrypted representation of the password.
     */
    String getEncryptedPassword();

    /**
     * Sets the encrypted representation of the password encoded as base64.
     * @param encryptedPassword the encrypted representation of the password.
     */
    void setEncryptedPassword(String encryptedPassword);

    /**
     * Returns the unecrypted version of the password.
     * @return the unecrypted version of the password.
     */
    String getPassword();

    /**
     * Sets the unencrypted version of the password.
     * @param password the unencrypted version of the password.
     */
    void setPassword(String password);
}
