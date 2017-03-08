package org.xlrnet.datac.vcs.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.foundation.services.AbstractTransactionalService;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.repository.BranchRepository;

/**
 * Created by jhendess on 06.03.2017.
 */
@Service
public class BranchService extends AbstractTransactionalService<Branch, BranchRepository> {

    /**
     * Constructor for abstract transactional service. Needs always a crud repository for performing operations.
     *
     * @param crudRepository
     *         The crud repository for providing basic crud operations.
     */
    @Autowired
    public BranchService(BranchRepository crudRepository) {
        super(crudRepository);
    }
}
