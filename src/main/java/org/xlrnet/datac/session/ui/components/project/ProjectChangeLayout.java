package org.xlrnet.datac.session.ui.components.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.label.MLabel;
import org.vaadin.viritin.layouts.MCssLayout;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.ui.DatacTheme;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.services.ChangeSetService;
import org.xlrnet.datac.foundation.ui.components.CodeSnippet;
import org.xlrnet.datac.foundation.ui.services.NavigationService;
import org.xlrnet.datac.foundation.ui.util.RevisionFormatService;
import org.xlrnet.datac.vcs.domain.Revision;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Layout component which is used for displaying the changes of a project revision.
 */
@SpringComponent
@Scope("prototype")
@JavaScript("vaadin://vendor/prism/prism.js")
@StyleSheet("vaadin://vendor/prism/prism.css")
public class ProjectChangeLayout extends AbstractProjectLayout {

    private static final int REVISIONS_TO_TRAVERSE = 200;   // TODO: Move this to config

    private VerticalLayout changeListLayout;

    /** Service for accessing change sets. */
    private final ChangeSetService changeSetService;

    /** Service for formatting revisions. */
    private final RevisionFormatService revisionFormatService;

    /** Service for navigation. */
    private final NavigationService navigationService;

    /** Map which contains all code snippets that may be highlighted. */
    private Map<DatabaseChangeSet, CodeSnippet> codeSnippetMap;

    /** Number of changesets in this revision. */
    private int changeSetSize;

    /** List of all detailed change set layouts. */
    private List<Component> detailedChangeSetLayouts = new ArrayList<>(0);

    /** Window for starting a quick deployment. */
    private final QuickDeploymentWindow quickDeploymentWindow;

    @Autowired
    public ProjectChangeLayout(ChangeSetService changeSetService, RevisionFormatService revisionFormatService, NavigationService navigationService, QuickDeploymentWindow quickDeploymentWindow) {
        this.changeSetService = changeSetService;
        this.revisionFormatService = revisionFormatService;
        this.navigationService = navigationService;
        this.quickDeploymentWindow = quickDeploymentWindow;
    }

    @Override
    void initialize() {
        changeListLayout = new MVerticalLayout().withStyleName("change-list");
        add(changeListLayout);
    }

    @Override
    public void beforeContentRefresh() {
        for (Component detailedChangeSetLayout : detailedChangeSetLayouts) {
            detailedChangeSetLayout.setVisible(false);
        }
    }

    @Override
    protected void refreshContent() throws DatacTechnicalException {
        List<DatabaseChangeSet> changeSets = changeSetService.findDatabaseChangeSetsInRevision(getRevision(), REVISIONS_TO_TRAVERSE);
        changeSetSize = changeSets.size();
        Collections.reverse(changeSets);
        refreshChangeSetList(changeSets);
    }

    private void refreshChangeSetList(List<DatabaseChangeSet> changeSets) {
        codeSnippetMap = new HashMap<>(changeSetSize);
        detailedChangeSetLayouts = new ArrayList<>(changeSetSize);
        changeListLayout.removeAllComponents();
        for (DatabaseChangeSet changeSet : changeSets) {
            MCssLayout changeLayout = buildComponentForSingleChangeSet(changeSet);
            changeListLayout.addComponent(changeLayout);
        }
    }

    @NotNull
    private MCssLayout buildComponentForSingleChangeSet(DatabaseChangeSet changeSet) {
        String title = changeSetService.formatDatabaseChangeSetTitle(changeSet);
        MCssLayout changeLayout = new MCssLayout().withStyleName("change", "card", "card-2");
        MLabel changeSetLabel = new MLabel(title).withStyleName(ValoTheme.LABEL_BOLD);
        changeLayout.add(changeSetLabel);
        if (changeSet.isModifying()) {
            changeLayout.setDescription("This change set was modified after check-in.");
            changeSetLabel.setIcon(VaadinIcons.BOLT);
        } else if (changeSet.getIntroducingChangeSet() == null && Objects.equals(changeSet.getRevision().getId(), getRevision().getId())) {
            changeLayout.setDescription("This change set was introduced in the current revision.");
            changeSetLabel.setIcon(VaadinIcons.PLUS);
        } else {
            changeSetLabel.setIcon(VaadinIcons.DATABASE);
        }

        MVerticalLayout detailedChangeSet = buildDetailedPanel(changeSet);
        detailedChangeSet.setVisible(false);
        changeLayout.add(detailedChangeSet);
        detailedChangeSetLayouts.add(detailedChangeSet);

        // Show or hide the detailed layout on click and format the preview
        changeLayout.addLayoutClickListener(e -> {
            detailedChangeSet.setVisible(!detailedChangeSet.isVisible());
            if (detailedChangeSet.isVisible() && codeSnippetMap.containsKey(changeSet)) {
                codeSnippetMap.get(changeSet).formatWithPrism();
            }
        });
        return changeLayout;
    }

    @NotNull
    private MVerticalLayout buildDetailedPanel(DatabaseChangeSet changeSet) {
        MVerticalLayout panelContent = new MVerticalLayout().withStyleName("change-detail-container").withFullWidth();
        MHorizontalLayout topContainer = new MHorizontalLayout().withMargin(false).withFullWidth();
        panelContent.addComponent(topContainer);
        GridLayout grid = new GridLayout(2, 6);
        grid.setMargin(false);
        grid.setWidth(DatacTheme.FULL_SIZE);
        grid.addStyleName("change-detail-grid");

        DatabaseChangeSet firstChangeSet = changeSet.getIntroducingChangeSet() != null ? changeSet.getIntroducingChangeSet() : changeSet;
        Revision firstRevision = firstChangeSet.getRevision();
        grid.addComponent(new MLabel("Created revision: "));
        grid.addComponent(new MButton(revisionFormatService.formatMessage(firstRevision))
                .withStyleName(ValoTheme.BUTTON_LINK)
                .withListener(e -> navigationService.openRevisionView(firstRevision)));
        grid.addComponent(new MLabel("Created by: "));
        grid.addComponent(new MLabel(revisionFormatService.formatAuthor(firstRevision)));    // TODO: Map the authors to actual users
        if (StringUtils.isNotBlank(firstRevision.getReviewer())) {
            grid.addComponent(new MLabel("Reviewed by: "));
            grid.addComponent(new MLabel(revisionFormatService.formatReviewer(firstRevision)));
        }
        grid.addComponent(new MLabel("Created at: "));
        grid.addComponent(new MLabel(revisionFormatService.formatTimestamp(firstRevision)));

        MVerticalLayout buttonLayout = new MVerticalLayout().withMargin(false);
        buttonLayout.add(new MButton("Quick deploy...").withListener((e) -> {
            quickDeploymentWindow.prepareWindow(changeSet, getRevision());
            UI.getCurrent().addWindow(quickDeploymentWindow);
        }));


        topContainer.with(grid).withExpand(grid, 0.85f);
        topContainer.with(buttonLayout).withExpand(buttonLayout, 0.15f);

        /* Add information about modification if the currently processed changeset was modified by a previous one. */
        if (changeSet.isModifying()) {
            Revision conflictingRevision = changeSet.getRevision();
            grid.addComponent(new MLabel("Modified revision: "));
            grid.addComponent(new MButton(revisionFormatService.formatMessage(conflictingRevision))
                    .withStyleName(ValoTheme.BUTTON_LINK)
                    .withListener(e -> navigationService.openRevisionView(conflictingRevision)));
            grid.addComponent(new MLabel("Modified by: "));
            grid.addComponent(new MLabel(revisionFormatService.formatAuthor(conflictingRevision)));   // TODO: Map the authors to actual users
            if (StringUtils.isNotBlank(conflictingRevision.getReviewer())) {
                grid.addComponent(new MLabel("Modification reviewed by: "));
                grid.addComponent(new MLabel(revisionFormatService.formatReviewer(conflictingRevision)));
            }
            grid.addComponent(new MLabel("Modified at: "));
            grid.addComponent(new MLabel(revisionFormatService.formatTimestamp(conflictingRevision)));
        }

        if (!changeSet.getChanges().isEmpty() && StringUtils.isNotBlank(changeSet.getChanges().get(0).getPreviewSql())) {
            grid.addComponent(new Label("SQL Preview:"));
            String previewSql = changeSetService.formatPreviewSql(changeSet).trim();
            CodeSnippet sqlPreview = new CodeSnippet(previewSql);
            sqlPreview.addStyleName("sql-preview");
            codeSnippetMap.put(changeSet, sqlPreview);
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
