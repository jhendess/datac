package org.xlrnet.datac.database.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.AbstractSpringBootTest;
import org.xlrnet.datac.database.domain.DatabaseConnection;
import org.xlrnet.datac.test.domain.EntityCreatorUtil;

public class DatabaseConnectionServiceTest extends AbstractSpringBootTest  {

    private static final String SAMPLE_PASSWORD = "MY_HIDDEN_PASSWORD_WITH_MAXIMUM_ALLOWED_CHARACTERS";

    @Autowired
    DatabaseConnectionService connectionService;

    @Test
    public void testTransparentPasswordEncryption_onlyPWUpdate() {
        DatabaseConnection databaseConnection = EntityCreatorUtil.buildDatabaseConnection();
        databaseConnection.setPassword(SAMPLE_PASSWORD);

        connectionService.save(databaseConnection);

        DatabaseConnection loaded = connectionService.findOne(databaseConnection.getId());
        loaded.setPassword("NEW_PASSWORD");
        DatabaseConnection saved = connectionService.save(loaded);

        assertEquals("Passwords don't match after updating", "NEW_PASSWORD", saved.getPassword());
    }

    @Test
    public void testSaveEncryptedConnection() {
        DatabaseConnection connection = EntityCreatorUtil.buildDatabaseConnection();

        DatabaseConnection savedEntity = connectionService.save(connection);

        assertNotNull("Persisted id may not be null", savedEntity.getId());
        assertNotNull("Encrypted password may not be null", savedEntity.getEncryptedPassword());

        DatabaseConnection fetchedEntity = connectionService.findOne(savedEntity.getId());

        assertEquals("Clear-text password doesn't match", connection.getPassword(), fetchedEntity.getPassword());
        assertEquals("Encrypted password doesn't match", savedEntity.getEncryptedPassword(), fetchedEntity.getEncryptedPassword());
        assertEquals(DatatypeConverter.printHexBinary(savedEntity.getSalt()), DatatypeConverter.printHexBinary(fetchedEntity.getSalt()));
    }

}