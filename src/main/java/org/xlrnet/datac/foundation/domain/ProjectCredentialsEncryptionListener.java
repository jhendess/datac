package org.xlrnet.datac.foundation.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.util.BeanInjector;
import org.xlrnet.datac.session.services.CryptoService;

import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;

/**
 * Entity listener which is responsible for encrypting and decrypting credentials in {@link Project} entities
 * transparently.
 */
@Component
public class ProjectCredentialsEncryptionListener {

    private CryptoService cryptoService;

    @Autowired
    public ProjectCredentialsEncryptionListener(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public ProjectCredentialsEncryptionListener() {
    }

    /**
     * Decrypt password after loading.
     */
    @PostLoad
    @PostUpdate
    public void decrypt(Project project) {
        autowireCryptoServiceIfNecessary();
        setSaltIfNecessary(project);
        String decrypted = cryptoService.decryptString(project.getEncryptedPassword(), project.getSalt());
        project.setPassword(decrypted);
    }

    /**
     * Encrypt password before persisting. (Currently done manually)
     */
    /*@PrePersist
    @PreUpdate*/
    public void encrypt(Project project) {
        autowireCryptoServiceIfNecessary();
        setSaltIfNecessary(project);
        String encrypted = cryptoService.encryptString(project.getPassword(), project.getSalt());
        project.setEncryptedPassword(encrypted);
    }

    private void setSaltIfNecessary(Project project) {
        if (project.getSalt() == null || project.getSalt().length == 0) {
            byte[] salt = cryptoService.generateSafeRandom(32);
            project.setSalt(salt);
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
