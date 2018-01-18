package org.xlrnet.datac.session.ui.components.project;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.vaadin.viritin.label.MLabel;
import org.vaadin.viritin.layouts.MCssLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.foundation.ui.util.RevisionFormatService;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.Image;

/**
 * Layout component which is used for displaying the history of a project' revisions.
 */
@SpringComponent
@Scope("prototype")
public class ProjectRevisionLayout extends AbstractProjectLayout {

    private static final int MAX_REVISIONS_TO_DISPLAY = 50;

    /** Service for accessing the revision graph. */
    private final RevisionGraphService revisionGraphService;

    /** Service for formatting revision data. */
    private final RevisionFormatService revisionFormatService;

    /** List of revisions. */
    private MVerticalLayout revisionList;

    @Autowired
    public ProjectRevisionLayout(RevisionGraphService revisionGraphService, RevisionFormatService revisionFormatService) {
        super();
        this.revisionGraphService = revisionGraphService;
        this.revisionFormatService = revisionFormatService;
    }

    @Override
    void initialize() {
        revisionList = new MVerticalLayout().withStyleName("revision-list");
        addComponent(revisionList);
    }

    @Override
    public void beforeContentRefresh() {
        // No action necessary
    }

    @Override
    protected void refreshContent() {
        revisionList.removeAllComponents();
        List<Revision> revisions = revisionGraphService.flattenRevisionGraph(getRevision(), MAX_REVISIONS_TO_DISPLAY);

        for (Revision rev : revisions) {
            MCssLayout singleRevision = buildLayoutForRevision(rev);
            revisionList.add(singleRevision);
        }
    }

    @NotNull
    private MCssLayout buildLayoutForRevision(Revision rev) {
        MCssLayout singleRevision = new MCssLayout().withStyleName("revision", "card", "card-2");

        MCssLayout avatarLayout = new MCssLayout().withStyleName("author-avatar");
        Image avatarImage = new Image(null, new ThemeResource(
                "img/profile-pic-300px.jpg"));  // TODO: Use actual profile image
        avatarImage.addStyleName("author-avatar");
        avatarImage.addStyleName("round-image");
        avatarLayout.add(avatarImage);

        MVerticalLayout revisionInfo = new MVerticalLayout()
                .withMargin(false).withSpacing(false).withUndefinedWidth()
                .withStyleName("revision-info-container");
        revisionInfo.add(new MLabel(revisionFormatService.formatMessage(rev)).withStyleName("revision-message"));

        MCssLayout authorAndDate = new MCssLayout().withStyleName("revision-author-time-container");
        authorAndDate.add(new MLabel("Committed by&nbsp;").withContentMode(ContentMode.HTML));
        authorAndDate.add(new MLabel(revisionFormatService.formatAuthor(rev)).withDescription(rev.getAuthor()).withStyleName("revision-author"));
        authorAndDate.add(new MLabel("&nbsp;on&nbsp;").withContentMode(ContentMode.HTML));
        authorAndDate.add(new MLabel(revisionFormatService.formatTimestamp(rev)).withStyleName("revision-timestamp"));
        revisionInfo.add(authorAndDate);

        MCssLayout rightContainer = new MCssLayout().withStyleName("revision-right-container")
                .withComponent(new MLabel(revisionFormatService.abbreviateRevisionId(rev)).withStyleName("revision-id"));

        singleRevision.withComponent(avatarLayout).withComponent(revisionInfo).withComponent(rightContainer);
        return singleRevision;
    }

    @NotNull
    @Override
    public String getSubtitle() {
        String revisionNumber = revisionFormatService.abbreviateRevisionId(getRevision());
        String revisionMessage = StringUtils.substringBefore(this.getRevision().getMessage(), "\n");
        return String.format("You are viewing the history of revision %s (%s).", revisionNumber, revisionMessage);
    }

    @NotNull
    @Override
    public String getTitle() {
        if (getBranch() != null) {
            return String.format("Revision graph of %s on branch %s", getProject().getName(), getBranch().getName());
        } else {
            return String.format("Revisions graph of %s in revision %s", getProject().getName(), revisionFormatService.abbreviateRevisionId(getRevision()));
        }
    }
}
