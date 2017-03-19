package org.xlrnet.datac.vcs.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.vcs.domain.repository.RevisionRepository;

/**
 * Service for accessing and manipulating VCS revision graphs.
 */
@Service
public class RevisionGraphService extends AbstractTransactionalService {

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *
     * @param crudRepository
     *         The crud repository for providing basic crud operations.
     */
    @Autowired
    public RevisionGraphService(RevisionRepository crudRepository) {
        super(crudRepository);
    }
}
