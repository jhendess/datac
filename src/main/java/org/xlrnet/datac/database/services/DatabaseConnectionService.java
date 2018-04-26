package org.xlrnet.datac.database.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlrnet.datac.database.domain.DatabaseConnection;
import org.xlrnet.datac.database.domain.repository.DatabaseConnectionRepository;
import org.xlrnet.datac.foundation.domain.PasswordEncryptionListener;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;

import lombok.extern.slf4j.Slf4j;

/**
 * Transactional service for accessing database configration data.
 */
@Slf4j
@Service
public class DatabaseConnectionService extends AbstractTransactionalService<DatabaseConnection, DatabaseConnectionRepository> {

    /**
     * Since hibernate only calls entity listeners when a non-transient field has changed, we have to call the listener
     * manually on save operations which is pretty ugly imho...
     */
    private final PasswordEncryptionListener passwordEncryptionListener;

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *
     * @param crudRepository             The crud repository for providing basic crud operations.
     */
    @Autowired
    public DatabaseConnectionService(DatabaseConnectionRepository crudRepository, PasswordEncryptionListener passwordEncryptionListener) {
        super(crudRepository);
        this.passwordEncryptionListener = passwordEncryptionListener;
    }

    @Override
    public <S extends DatabaseConnection> S save(S entity) {
        passwordEncryptionListener.encrypt(entity);
        return super.save(entity);
    }

    /**
     * Finds all database connections ordered by their name.
     *
     * @return all database connections ordered by their name.
     */
    @Transactional(readOnly = true)
    public List<DatabaseConnection> findAllOrderByNameAsc() {
        return getRepository().findAllByOrderByName();
    }
}
