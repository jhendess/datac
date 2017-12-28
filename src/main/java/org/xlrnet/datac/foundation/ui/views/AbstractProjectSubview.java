package org.xlrnet.datac.foundation.ui.views;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.IllegalUIStateException;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.BranchService;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

public abstract class AbstractProjectSubview extends AbstractSubview {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProjectSubview.class);

    /**
     * Number of revisions to traverse in order to find database changes.
     */
    static final int REVISIONS_TO_TRAVERSE = 200;


    private static final String BRANCH_PARAMETER = "branch";

    /**
     * Service for accessing revision graph.
     */
    final RevisionGraphService revisionGraphService;

    /**
     * Service for accessing branch data.
     */
    final BranchService branchService;

    /**
     * The current project.
     */
    Project project;

    /**
     * The current branch.
     */
    Branch branch;

    /**
     * The revision which is effectively displayed.
     */
    Revision revision;

    protected AbstractProjectSubview(RevisionGraphService revisionGraphService, BranchService branchService) {
        this.revisionGraphService = revisionGraphService;
        this.branchService = branchService;
    }

    @Override
    protected void initialize() throws DatacTechnicalException {
        Long revisionId = null;
        if (getParameters().length == 1 && NumberUtils.isDigits(getParameters()[0])) {
            revisionId = Long.valueOf(getParameters()[0]);
            revision = revisionGraphService.findOne(revisionId);
        }
        if (revision == null) {
            LOGGER.warn("Unable to find revision {}", revisionId);
            throw new IllegalUIStateException("Unable to find revision " + revisionId, getViewName(), getParameters());
        } else {
            project = revision.getProject();
        }

        String branchParameter = getNamedParameter(BRANCH_PARAMETER);
        if (branchParameter != null) {
            branch = branchService.findOne(Long.valueOf(branchParameter));
        }
    }

    protected abstract String getViewName();

}
