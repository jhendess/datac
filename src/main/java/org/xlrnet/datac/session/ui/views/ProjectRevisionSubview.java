package org.xlrnet.datac.session.ui.views;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.label.MLabel;
import org.vaadin.viritin.layouts.MCssLayout;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.foundation.ui.services.NavigationService;
import org.xlrnet.datac.foundation.ui.util.RevisionFormatService;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.BranchService;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

import com.vaadin.data.HasValue;
import com.vaadin.server.ThemeResource;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.Image;
import com.vaadin.ui.Layout;

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
        MVerticalLayout layout = new MVerticalLayout();
        layout.addStyleName("revision-list");

        for (Revision rev : revisions) {
            MCssLayout singleRevision = buildLayoutForRevision(rev);
            layout.add(singleRevision);
        }

        mainPanel.addComponent(layout);
    }

    @NotNull
    private MCssLayout buildLayoutForRevision(Revision rev) {
        MCssLayout singleRevision = new MCssLayout().withStyleName("revision");

        MCssLayout avatarLayout = new MCssLayout().withStyleName("author-avatar");
        Image profilePic = new Image(null, new ThemeResource(
                "img/profile-pic-300px.jpg"));  // TODO: Use actual profile image
        profilePic.setStyleName("author-avatar");
        avatarLayout.add(profilePic);

        MVerticalLayout revisionInfo = new MVerticalLayout()
                .withMargin(false).withSpacing(false).withUndefinedWidth()
                .withStyleName("revision-info-container");
        revisionInfo.add(new MLabel(revisionFormatService.formatMessage(rev)).withStyleName("revision-message"));

        MCssLayout authorAndDate = new MCssLayout().withStyleName("revision-author-time-container");
        authorAndDate.add(new MLabel("Committed by "));
        authorAndDate.add(new MLabel(revisionFormatService.formatAuthor(rev)).withDescription(revision.getAuthor()).withStyleName("revision-author"));
        authorAndDate.add(new MLabel(" on "));
        authorAndDate.add(new MLabel(revisionFormatService.formatTimestamp(rev)).withStyleName("revision-timestamp"));
        revisionInfo.add(authorAndDate);

        MCssLayout rightContainer = new MCssLayout().withStyleName("revision-right-container")
                .withComponent(new MLabel(revisionFormatService.abbreviateRevisionId(rev)).withStyleName("revision-id"));

        singleRevision.withComponent(avatarLayout).withComponent(revisionInfo).withComponent(rightContainer);
        return singleRevision;
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
