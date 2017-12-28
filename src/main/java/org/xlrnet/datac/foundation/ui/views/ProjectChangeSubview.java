package org.xlrnet.datac.foundation.ui.views;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.stackpanel.StackPanel;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.services.ChangeSetService;
import org.xlrnet.datac.foundation.ui.services.NavigationService;
import org.xlrnet.datac.foundation.ui.util.FormatUtils;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.BranchService;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Renders a list of {@link org.xlrnet.datac.database.domain.DatabaseChangeSet} in a given branch/revision. The view
 * does not display the actual change sets in the given revision, but the change sets which are present in the closest
 * revision
 */
@SpringComponent
@SpringView(name = ProjectChangeSubview.VIEW_NAME)
public class ProjectChangeSubview extends AbstractProjectSubview {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectChangeSubview.class);

    public static final String VIEW_NAME = "project/changes";

    /**
     * Service for accessing database changes.
     */
    private final ChangeSetService changeSetService;

    /**
     * Service for navigating to various view states.
     */
    private final NavigationService navigationService;

    /**
     * The change sets which will be displayed.
     */
    private List<DatabaseChangeSet> changeSets;

    @Autowired
    public ProjectChangeSubview(BranchService branchService, RevisionGraphService revisionGraphService, ChangeSetService changeSetService, NavigationService navigationService) {
        super(revisionGraphService, branchService);
        this.changeSetService = changeSetService;
        this.navigationService = navigationService;
    }

    @Override
    protected void initialize() throws DatacTechnicalException {
        super.initialize();

        changeSets = changeSetService.findDatabaseChangeSetsInRevision(revision, REVISIONS_TO_TRAVERSE);
        Collections.reverse(changeSets);
    }

    @Override
    protected String getViewName() {
        return VIEW_NAME;
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        MVerticalLayout mainPanel = new MVerticalLayout();
        mainPanel.add(buildNavigationPanel());
        mainPanel.add(buildChangeSetList());

        return mainPanel;
    }

    private Component buildNavigationPanel() {
        MHorizontalLayout navigationPanel = new MHorizontalLayout();

        NativeSelect<Branch> branchSelector = new NativeSelect<>();
        List<Branch> branchList = branchService.findAllWatchedByProject(project);
        if (!branchList.contains(branch)) {
            branchList.add(branch);
        }
        Collections.sort(branchList);
        branchSelector.setItems(branchList);
        branchSelector.setSelectedItem(branch);
        branchSelector.setEmptySelectionAllowed(false);
        branchSelector.addValueChangeListener(e -> navigationService.openChangeView(e.getValue()));
        branchSelector.setItemCaptionGenerator(Branch::getName);
        branchSelector.setCaption("Change branch");
        branchSelector.setIcon(VaadinIcons.ROAD_BRANCH);
        navigationPanel.add(branchSelector);

        Button revisionBrowserButton = new Button("Open revision graph");
        revisionBrowserButton.addClickListener(e -> navigationService.openRevisionView(revision, branch));
        navigationPanel.add(revisionBrowserButton);

        return navigationPanel.alignAll(Alignment.BOTTOM_LEFT);
    }

    @NotNull
    private Component buildChangeSetList() {
        VerticalLayout changeListLayout = new MVerticalLayout();

        for (DatabaseChangeSet changeSet : changeSets) {
            String title = changeSetService.formatDatabaseChangeSetTitle(changeSet);
            Panel panel = new Panel(title);
            if (changeSet.isModifying()) {
                panel.setDescription("This change set was modified after check-in.");
                panel.setIcon(VaadinIcons.BOLT);
            } else if (changeSet.getIntroducingChangeSet() == null && Objects.equals(changeSet.getRevision().getId(), revision.getId())) {
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
        grid.addComponent(new MButton(FormatUtils.formatRevisionWithMessage(firstRevision))
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
            grid.addComponent(new MButton(FormatUtils.formatRevisionWithMessage(conflictingRevision))
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
    protected String getSubtitle() {
        String subtitle;
        if (changeSets.isEmpty()) {
            subtitle = "There are no database changes in this revision. Make sure " +
                    "that the project is configured correctly so that database changes can be found.";
        } else if (revision == null) {
            subtitle = "There is no revision on this branch. Make sure that you ran a project update first.";
        } else {
            String revisionNumber = FormatUtils.abbreviateRevisionId(revision);
            String revisionMessage = StringUtils.substringBefore(this.revision.getMessage(), "\n");
            subtitle = String.format("There are currently %d database changes in revision %s (%s).", changeSets.size(), revisionNumber, revisionMessage);
        }
        return subtitle;
    }

    @NotNull
    @Override
    protected String getTitle() {
        if (branch != null) {
            return String.format("Change sets in %s on branch %s", project.getName(), branch.getName());
        } else {
            return String.format("Change sets in %s in revision %s", project.getName(), FormatUtils.abbreviateRevisionId(revision));
        }
    }
}
