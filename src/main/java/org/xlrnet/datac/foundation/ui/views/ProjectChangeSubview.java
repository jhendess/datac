package org.xlrnet.datac.foundation.ui.views;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.stackpanel.StackPanel;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.IllegalUIStateException;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.services.ChangeSetService;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.BranchService;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Renders a list of {@link org.xlrnet.datac.database.domain.DatabaseChangeSet} in a given branch/revision. The view does not display the actual change sets in the given revision, but the change sets which are present in the closest revision
 */
@SpringComponent
@SpringView(name = ProjectChangeSubview.VIEW_NAME)
public class ProjectChangeSubview extends AbstractSubview {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectChangeSubview.class);

    public static final String VIEW_NAME = "project/changes";

    private static final int REVISIONS_TO_TRAVERSE = 50;

    private static final int REVISION_LENGTH = 7;

    /** Service for accessing branch data. */
    private final BranchService branchService;

    /** Service for accessing revision graph. */
    private final RevisionGraphService revisionGraphService;

    /** Service for accessing database changes. */
    private final ChangeSetService changeSetService;

    /** The current project. */
    private Project project;

    /** The current branch. */
    private Branch branch;

    /** The revision which is effectively displayed. */
    private Revision revision;

    /** The change sets which will be displayed. */
    private List<DatabaseChangeSet> changeSets;

    @Autowired
    public ProjectChangeSubview(BranchService branchService, RevisionGraphService revisionGraphService, ChangeSetService changeSetService) {
        this.branchService = branchService;
        this.revisionGraphService = revisionGraphService;
        this.changeSetService = changeSetService;
    }

    @Override
    protected void initialize() throws DatacTechnicalException {
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

        revision = revisionGraphService.findLastRevisionOnBranch(branch);
        changeSets = changeSetService.findLastDatabaseChangeSetsOnBranch(branch, REVISIONS_TO_TRAVERSE);
        Collections.reverse(changeSets);
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {

        return buildChangeSetList();
    }

    @NotNull
    private VerticalLayout buildChangeSetList() {
        VerticalLayout changeListLayout = new MVerticalLayout();

        for (DatabaseChangeSet changeSet : changeSets) {
            String title = changeSetService.formatDatabaseChangeSetTitle(changeSet);
            Panel panel = new Panel(title);
            DatabaseChangeSet firstChangeSet = changeSet.getIntroducingChangeSet() != null ? changeSet.getIntroducingChangeSet() : changeSet;
            DatabaseChangeSet conflictingChangeSet = firstChangeSet.getConflictingChangeSet();  // FIXME: What happens if there are multiple overwritten change sets?
            if (conflictingChangeSet != null) {
                panel.setDescription("This change set was modified after check-in.");
                panel.setIcon(VaadinIcons.BOLT);
            } else if (changeSet.getIntroducingChangeSet() == null && Objects.equals(changeSet.getRevision().getId(), revision.getId())) {
                panel.setDescription("This change set was introduced in this revision.");
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

        return changeListLayout;
    }

    @NotNull
    private VerticalLayout buildDetailedPanel(DatabaseChangeSet changeSet) {
        VerticalLayout panelContent = new VerticalLayout();
        GridLayout grid = new GridLayout(2, 6);
        panelContent.addComponent(grid);

        DatabaseChangeSet firstChangeSet = changeSet.getIntroducingChangeSet() != null ? changeSet.getIntroducingChangeSet() : changeSet;
        Revision firstRevision = firstChangeSet.getRevision();
        grid.addComponent(new Label("Created revision: "));
        grid.addComponent(new Label(formatRevision(firstRevision)));
        grid.addComponent(new Label("Created by: "));
        grid.addComponent(new Label(firstRevision.getAuthor()));    // TODO: Map the authors to actual users
        if (StringUtils.isNotBlank(firstRevision.getReviewer())) {
            grid.addComponent(new Label("Reviewed by: "));
            grid.addComponent(new Label(firstRevision.getReviewer()));
        }
        grid.addComponent(new Label("Created at: "));
        grid.addComponent(new Label(firstRevision.getCommitTime().toString()));

        DatabaseChangeSet conflictingChangeSet = firstChangeSet.getConflictingChangeSet();  // FIXME: What happens if there are multiple overwritten change sets?
        if (conflictingChangeSet != null) {
            Revision conflictingRevision = conflictingChangeSet.getRevision();
            grid.addComponent(new Label("Modified revision: "));
            grid.addComponent(new Label(formatRevision(conflictingRevision)));
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
            String previewSql = new BasicFormatterImpl().format(changeSet.getChanges().get(0).getPreviewSql());
            Label sqlPreview = new Label(previewSql, ContentMode.PREFORMATTED);
            sqlPreview.setWidth("80%");
            grid.addComponent(sqlPreview);
        } else {
            grid.addComponent(new Label("No SQL preview available"));
        }

        if (changeSet.getConflictingChangeSet() != null) {
            panelContent.addComponent(new Label("Warning: this change set is modified in a later revision."));
        }

        return panelContent;
    }

    @NotNull
    private String formatRevision(Revision firstRevision) {
        return StringUtils.substring(firstRevision.getInternalId(), 0, REVISION_LENGTH) + " - " + StringUtils.substringBefore(firstRevision.getMessage(), "\n");
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        String subtitle;
        if (changeSets.isEmpty()) {
            subtitle = "There are no database changes in this revision. Make sure " +
                    "that the project is configured correctly so that database changes can be found.";
        } else if (revision == null) {
            subtitle = "There is no revision on this branch. Make sure that you ran a project update first.";
        } else {
            String revisionNumber = StringUtils.substring(this.revision.getInternalId(), 0, REVISION_LENGTH);
            String revisionMessage = StringUtils.substringBefore(this.revision.getMessage(), "\n");
            subtitle = String.format("There are currently %d database changes in revision %s (%s).", changeSets.size(), revisionNumber, revisionMessage);
        }
        return subtitle;
    }

    @NotNull
    @Override
    protected String getTitle() {
        return "Change sets in project " + project.getName() + " on branch " + branch.getName();
    }
}
