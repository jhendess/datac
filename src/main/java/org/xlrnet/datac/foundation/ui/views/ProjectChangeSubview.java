package org.xlrnet.datac.foundation.ui.views;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.commons.exception.IllegalUIStateException;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.services.BranchService;

/**
 * Renders a list of {@link org.xlrnet.datac.database.domain.DatabaseChangeSet} in a given branch/revision.
 */
@SpringComponent
@SpringView(name = ProjectChangeSubview.VIEW_NAME)
public class ProjectChangeSubview extends AbstractSubview {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectChangeSubview.class);

    public static final String VIEW_NAME = "project/changes";

    /** Service for accessing branch data. */
    private final BranchService branchService;

    /** The current project. */
    private Project project;

    /** The current branch. */
    private Branch branch;

    @Autowired
    public ProjectChangeSubview(BranchService branchService) {
        this.branchService = branchService;
    }

    @Override
    protected void initialize() {
        Long branchId = null;
        if (getParameters().length == 1 && NumberUtils.isDigits(getParameters()[0])) {
            branchId = Long.valueOf(getParameters()[0]);
            branch = branchService.findOne(branchId);
        }
        if (branch == null) {
            LOGGER.warn("Unable to find branch {}", branchId);
            throw new IllegalUIStateException(VIEW_NAME, getParameters());
        } else {
            project = branch.getProject();
        }
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        return new VerticalLayout();
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        return "View all database changes in project in a project.";
    }

    @NotNull
    @Override
    protected String getTitle() {
        return "Change sets in project " + project.getName() + " on branch " + branch.getName();
    }
}
