package org.xlrnet.datac.session.ui.views;

import com.vaadin.data.HasValue;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.Layout;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.label.MLabel;
import org.vaadin.viritin.layouts.MGridLayout;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.xlrnet.datac.foundation.ui.services.NavigationService;
import org.xlrnet.datac.foundation.ui.util.RevisionFormatService;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.BranchService;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

import java.util.List;

/**
 * Renders the history graph of revisions before the revision to display.
 */
@SpringComponent
@SpringView(name = ProjectRevisionSubview.VIEW_NAME)
public class ProjectRevisionSubview extends AbstractProjectSubview {

    public static final String VIEW_NAME = "project/revisions";

    private static final int MAX_REVISIONS_TO_DISPLAY = 50;

    @Autowired
    protected ProjectRevisionSubview(RevisionGraphService revisionGraphService, BranchService branchService, RevisionFormatService revisionFormatService, NavigationService navigationService) {
        super(revisionGraphService, branchService, revisionFormatService, navigationService);
    }

    @Override
    protected String getViewName() {
        return VIEW_NAME;
    }

    @Override
    void extendNavigationPanel(MHorizontalLayout navigationPanel) {
        Button revisionBrowserButton = new Button("Show database changes");
        revisionBrowserButton.addClickListener(e -> navigationService.openChangeView(revision, branch));
        navigationPanel.add(revisionBrowserButton);
    }

    @Override
    protected void handleBranchSelectionChange(HasValue.ValueChangeEvent<Branch> e) {
        navigationService.openRevisionView(e.getValue());
    }

    @Override
    void buildContent(Layout mainPanel) {
        List<Revision> revisions = revisionGraphService.flattenRevisionGraph(revision, MAX_REVISIONS_TO_DISPLAY);
        MGridLayout gridLayout = new MGridLayout(4, revisions.size() + 1);
        gridLayout.addStyleName("revision-list-table");

        gridLayout.add(new MLabel("Author").withStyleName(ValoTheme.LABEL_BOLD));
        gridLayout.add(new MLabel("ID").withStyleName(ValoTheme.LABEL_BOLD));
        gridLayout.add(new MLabel("Message").withStyleName(ValoTheme.LABEL_BOLD));
        gridLayout.add(new MLabel("Timestamp").withStyleName(ValoTheme.LABEL_BOLD));

        for (Revision rev : revisions) {
            gridLayout.add(new MLabel(revisionFormatService.formatAuthor(rev)).withDescription(revision.getAuthor()));
            gridLayout.add(new MLabel(revisionFormatService.abbreviateRevisionId(rev)));
            gridLayout.add(new MLabel(revisionFormatService.formatMessage(rev)));
            gridLayout.add(new MLabel(revisionFormatService.formatTimestamp(rev)));
        }

        mainPanel.addComponent(gridLayout);
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        String revisionNumber = revisionFormatService.abbreviateRevisionId(revision);
        String revisionMessage = StringUtils.substringBefore(this.revision.getMessage(), "\n");
        return String.format("You are viewing the history of revision %s (%s).", revisionNumber, revisionMessage);
    }

    @NotNull
    @Override
    protected String getTitle() {
        if (branch != null) {
            return String.format("Revision graph of %s on branch %s", project.getName(), branch.getName());
        } else {
            return String.format("Revisions graph of %s in revision %s", project.getName(), revisionFormatService.abbreviateRevisionId(revision));
        }
    }
}
