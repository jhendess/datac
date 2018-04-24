package org.xlrnet.datac.foundation.domain;

import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.util.BeanInjector;
import org.xlrnet.datac.session.services.CryptoService;

import lombok.extern.slf4j.Slf4j;

/**
 * Entity listener which is responsible for encrypting and decrypting credentials in {@link Project} entities
 * transparently.
 */
@Slf4j
@Component
public class PasswordEncryptionListener {

    /** Service for performing encryption. */
    private CryptoService cryptoService;

    @Autowired
    public PasswordEncryptionListener(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public PasswordEncryptionListener() {
    }

    /**
     * Decrypt password after loading.
     */
    @PostLoad
    @PostUpdate
    public void decrypt(PasswordEncryptedEntity entity) {
        autowireCryptoServiceIfNecessary();
        setSaltIfNecessary(entity);
        String decrypted = cryptoService.decryptString(entity.getEncryptedPassword(), entity.getSalt());
        entity.setPassword(decrypted);
    }

    /**
     * Encrypt password before persisting. (Must currently be done manually because hibernate apparently doesn't run listeners when transient fields change)
     */
    /*@PrePersist
    @PreUpdate*/
    public void encrypt(PasswordEncryptedEntity entity) {
        autowireCryptoServiceIfNecessary();
        setSaltIfNecessary(entity);
        String encrypted = cryptoService.encryptString(entity.getPassword(), entity.getSalt());
        entity.setEncryptedPassword(encrypted);
    }

    private void setSaltIfNecessary(PasswordEncryptedEntity entity) {
        if (entity.getSalt() == null || entity.getSalt().length == 0) {
            LOGGER.debug("Generating new salt for entity");
            byte[] salt = cryptoService.generateSafeRandom(32);
            entity.setSalt(salt);
        }
    }

    /**
     * Inject the crypto service if not yet loaded. This is necessary since entity listeners are not managed by spring,
     * but by JPA.
     */
    private void autowireCryptoServiceIfNecessary() {
        if (cryptoService == null) {
            synchronized (this) {
                cryptoService = BeanInjector.getBean(CryptoService.class);
            }
        }
    }
}
