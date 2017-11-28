package org.xlrnet.datac.commons.exception;

import org.xlrnet.datac.foundation.domain.Project;

/**
 * The change system adapter for a given project is missing.
 */
public class MissingDatabaseChangeSystemAdapterException extends DatacTechnicalException {

    public MissingDatabaseChangeSystemAdapterException(Project project) {
        super("The change system adapter for project " + project.getName() + " is missing");
    }
}
