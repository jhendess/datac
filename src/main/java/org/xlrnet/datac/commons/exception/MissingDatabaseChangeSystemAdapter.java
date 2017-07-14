package org.xlrnet.datac.commons.exception;

import org.xlrnet.datac.foundation.domain.Project;

/**
 * The change system adapter for a given project is missing.
 */
public class MissingDatabaseChangeSystemAdapter extends DatacTechnicalException {

    private final Project project;

    public MissingDatabaseChangeSystemAdapter(Project project) {
        super("The change system adapter for project " + project.getName() + " is missing");
        this.project = project;
    }

    public Project getProject() {
        return project;
    }
}
