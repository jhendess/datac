package org.xlrnet.datac.session.ui.components.project;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.vaadin.addons.stackpanel.StackPanel;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.services.ChangeSetService;
import org.xlrnet.datac.foundation.ui.services.NavigationService;
import org.xlrnet.datac.foundation.ui.util.RevisionFormatService;
import org.xlrnet.datac.vcs.domain.Revision;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Layout component which is used for displaying the changes of a project revision.
 */
@SpringComponent
@Scope("prototype")
public class ProjectChangeLayout extends AbstractProjectLayout {

    private static final int REVISIONS_TO_TRAVERSE = 200;   // TODO: Move this to config

    private VerticalLayout changeListLayout;

    /** Service for accessing change sets. */
    private final ChangeSetService changeSetService;

    /** Service for formatting revisions. */
    private final RevisionFormatService revisionFormatService;

    /** Service for navigation. */
    private final NavigationService navigationService;

    private int changeSetSize;

    @Autowired
    public ProjectChangeLayout(ChangeSetService changeSetService, RevisionFormatService revisionFormatService, NavigationService navigationService) {
        this.changeSetService = changeSetService;
        this.revisionFormatService = revisionFormatService;
        this.navigationService = navigationService;
    }

    @Override
    void initialize() {
        changeListLayout = new MVerticalLayout();
        add(changeListLayout);
    }

    @Override
    protected void refreshContent() throws DatacTechnicalException {
        List<DatabaseChangeSet> changeSets = changeSetService.findDatabaseChangeSetsInRevision(getRevision(), REVISIONS_TO_TRAVERSE);
        changeSetSize = changeSets.size();
        Collections.reverse(changeSets);
        refreshChangeSetList(changeSets);
    }

    private void refreshChangeSetList(List<DatabaseChangeSet> changeSets) {
        changeListLayout.removeAllComponents();
        for (DatabaseChangeSet changeSet : changeSets) {
            String title = changeSetService.formatDatabaseChangeSetTitle(changeSet);
            Panel panel = new Panel(title);
            if (changeSet.isModifying()) {
                panel.setDescription("This change set was modified after check-in.");
                panel.setIcon(VaadinIcons.BOLT);
            } else if (changeSet.getIntroducingChangeSet() == null && Objects.equals(changeSet.getRevision().getId(), getRevision().getId())) {
                panel.setDescription("This change set was introduced in the current revision.");
                panel.setIcon(VaadinIcons.PLUS);
                panel.addStyleName(ValoTheme.LABEL_SUCCESS);
            } else {
                panel.setIcon(VaadinIcons.DATABASE);
            }

            VerticalLayout panelContent = buildDetailedPanel(changeSet);

            panel.setContent(panelContent);
            StackPanel stackPanel = StackPanel.extend(panel);
            stackPanel.close();
            changeListLayout.addComponent(panel);
        }
    }

    @NotNull
    private VerticalLayout buildDetailedPanel(DatabaseChangeSet changeSet) {
        VerticalLayout panelContent = new VerticalLayout();
        GridLayout grid = new GridLayout(2, 6);
        panelContent.addComponent(grid);

        DatabaseChangeSet firstChangeSet = changeSet.getIntroducingChangeSet() != null ? changeSet.getIntroducingChangeSet() : changeSet;
        Revision firstRevision = firstChangeSet.getRevision();
        grid.addComponent(new Label("Created revision: "));
        grid.addComponent(new MButton(revisionFormatService.formatRevisionWithMessage(firstRevision))
                .withStyleName(ValoTheme.BUTTON_LINK)
                .withListener(e -> navigationService.openRevisionView(firstRevision)));
        grid.addComponent(new Label("Created by: "));
        grid.addComponent(new Label(firstRevision.getAuthor()));    // TODO: Map the authors to actual users
        if (StringUtils.isNotBlank(firstRevision.getReviewer())) {
            grid.addComponent(new Label("Reviewed by: "));
            grid.addComponent(new Label(firstRevision.getReviewer()));
        }
        grid.addComponent(new Label("Created at: "));
        grid.addComponent(new Label(firstRevision.getCommitTime().toString()));

        if (changeSet.isModifying()) {
            Revision conflictingRevision = changeSet.getRevision();
            grid.addComponent(new Label("Modified revision: "));
            grid.addComponent(new MButton(revisionFormatService.formatRevisionWithMessage(conflictingRevision))
                    .withStyleName(ValoTheme.BUTTON_LINK)
                    .withListener(e -> navigationService.openRevisionView(conflictingRevision)));
            grid.addComponent(new Label("Modified by: "));
            grid.addComponent(new Label(conflictingRevision.getAuthor()));   // TODO: Map the authors to actual users
            if (StringUtils.isNotBlank(conflictingRevision.getReviewer())) {
                grid.addComponent(new Label("Modification reviewed by: "));
                grid.addComponent(new Label(conflictingRevision.getReviewer()));
            }
            grid.addComponent(new Label("Modified at: "));
            grid.addComponent(new Label(conflictingRevision.getCommitTime().toString()));
        }

        if (!changeSet.getChanges().isEmpty() && StringUtils.isNotBlank(changeSet.getChanges().get(0).getPreviewSql())) {
            grid.addComponent(new Label("SQL Preview:"));
            String previewSql = changeSetService.formatPreviewSql(changeSet);
            Label sqlPreview = new Label(previewSql, ContentMode.PREFORMATTED);
            sqlPreview.setWidth("80%");
            grid.addComponent(sqlPreview);
        } else {
            grid.addComponent(new Label("No SQL preview available"));
        }

        if (changeSetService.countModifyingChangeSets(changeSet) > 0) {
            Label conflictLabel = new Label("Warning: this change set is modified in a later revision.");
            conflictLabel.addStyleName(ValoTheme.NOTIFICATION_WARNING);
            panelContent.addComponent(conflictLabel);
        }

        return panelContent;
    }

    @NotNull
    @Override
    public String getSubtitle() {
        String subtitle;
        if (changeSetSize == 0) {
            subtitle = "There are no database changes in this revision. Make sure " +
                    "that the project is configured correctly so that database changes can be found.";
        } else {
            String revisionNumber = revisionFormatService.abbreviateRevisionId(getRevision());
            String revisionMessage = StringUtils.substringBefore(this.getRevision().getMessage(), "\n");
            subtitle = String.format("There are currently %d database changes in revision %s (%s).", changeSetSize, revisionNumber, revisionMessage);
        }
        return subtitle;
    }

    @NotNull
    @Override
    public String getTitle() {
        if (getBranch() != null) {
            return String.format("Change sets in %s on branch %s", getProject().getName(), getBranch().getName());
        } else {
            return String.format("Change sets in %s in revision %s", getProject().getName(), revisionFormatService.abbreviateRevisionId(getRevision()));
        }
    }
}
