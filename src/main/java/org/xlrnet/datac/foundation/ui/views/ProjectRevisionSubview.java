package org.xlrnet.datac.foundation.ui.views;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.foundation.ui.util.FormatUtils;
import org.xlrnet.datac.vcs.services.BranchService;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

@SpringComponent
@SpringView(name = ProjectRevisionSubview.VIEW_NAME)
public class ProjectRevisionSubview extends AbstractProjectSubview {

    public static final String VIEW_NAME = "project/revisions";

    @Autowired
    protected ProjectRevisionSubview(RevisionGraphService revisionGraphService, BranchService branchService) {
        super(revisionGraphService, branchService);
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        MVerticalLayout mainPanel = new MVerticalLayout();

        return mainPanel;
    }

    @Override
    protected String getViewName() {
        return VIEW_NAME;
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        String revisionNumber = FormatUtils.abbreviateRevisionId(revision);
        String revisionMessage = StringUtils.substringBefore(this.revision.getMessage(), "\n");
        String subtitle = String.format("You are viewing the history of revision %s (%s).", revisionNumber, revisionMessage);
        // TODO: Show author, reviewer, time, etc. and reuse this for the change graph
        return subtitle;
    }

    @NotNull
    @Override
    protected String getTitle() {
        if (branch != null) {
            return String.format("Revision graph of %s on branch %s", project.getName(), branch.getName());
        } else {
            return String.format("Revisions graph of %s in revision %s", project.getName(), FormatUtils.abbreviateRevisionId(revision));
        }
    }
}
