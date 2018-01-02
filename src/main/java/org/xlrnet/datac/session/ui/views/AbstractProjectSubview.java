package org.xlrnet.datac.session.ui.views;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.IllegalUIStateException;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.ui.services.NavigationService;
import org.xlrnet.datac.foundation.ui.util.RevisionFormatService;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.BranchService;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

import com.vaadin.data.HasValue;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.NativeSelect;

public abstract class AbstractProjectSubview extends AbstractSubview {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProjectSubview.class);

    /**
     * Number of revisions to traverse in order to find database changes.
     */
    static final int REVISIONS_TO_TRAVERSE = 200;

    /**
     * Parameter which may contain a branch name.
     */
    private static final String BRANCH_PARAMETER = "branch";

    /**
     * Service for accessing branch data.
     */
    private final BranchService branchService;

    /**
     * Service for navigating to various view states.
     */
    final NavigationService navigationService;

    /**
     * Service for accessing revision graph.
     */
    final RevisionGraphService revisionGraphService;

    /**
     * Service for formatting elements of a revision.
     */
    final RevisionFormatService revisionFormatService;

    /**
     * The current project.
     */
    Project project;

    /**
     * The current branch.
     */
    Branch branch;

    /**
     * The revision which is effectively displayed.
     */
    Revision revision;

    protected AbstractProjectSubview(RevisionGraphService revisionGraphService, BranchService branchService, RevisionFormatService revisionFormatService, NavigationService navigationService) {
        this.revisionGraphService = revisionGraphService;
        this.branchService = branchService;
        this.revisionFormatService = revisionFormatService;
        this.navigationService = navigationService;
    }

    Component buildNavigationPanel() {
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
        branchSelector.addValueChangeListener(this::handleBranchSelectionChange);
        branchSelector.setItemCaptionGenerator(Branch::getName);
        branchSelector.setCaption("Change branch");
        branchSelector.setIcon(VaadinIcons.ROAD_BRANCH);
        navigationPanel.add(branchSelector);

        extendNavigationPanel(navigationPanel);

        Button editProjectButton = new Button("Edit project");
        editProjectButton.addClickListener(e -> navigationService.openEditProjectView(project));
        navigationPanel.add(editProjectButton);

        return navigationPanel.alignAll(Alignment.BOTTOM_LEFT);
    }

    abstract void extendNavigationPanel(MHorizontalLayout navigationPanel);

    protected abstract void handleBranchSelectionChange(HasValue.ValueChangeEvent<Branch> e);

    @Override
    protected void initialize() throws DatacTechnicalException {
        Long revisionId = null;
        if (getParameters().length == 1 && NumberUtils.isDigits(getParameters()[0])) {
            revisionId = Long.valueOf(getParameters()[0]);
            revision = revisionGraphService.findOne(revisionId);
        }
        if (revision == null) {
            LOGGER.warn("Unable to find revision {}", revisionId);
            throw new IllegalUIStateException("Unable to find revision " + revisionId, getViewName(), getParameters());
        } else {
            project = revision.getProject();
        }

        String branchParameter = getNamedParameter(BRANCH_PARAMETER);
        if (branchParameter != null) {
            branch = branchService.findOne(Long.valueOf(branchParameter));
        }
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        MVerticalLayout mainPanel = new MVerticalLayout();
        mainPanel.setMargin(false);
        mainPanel.add(buildRevisionInfoPanel());
        mainPanel.addComponent(buildNavigationPanel());
        buildContent(mainPanel);
        return mainPanel;
    }

    private Layout buildRevisionInfoPanel() {
        GridLayout panelContent = new GridLayout(2, 5);

        panelContent.addComponent(new Label("Revision id: "));
        panelContent.addComponent(new Label(revision.getInternalId()));

        if (StringUtils.isBlank(revision.getMessage())) {
            panelContent.addComponent(new Label("Message: "));
            panelContent.addComponent(new Label(revision.getMessage()));
        }

        panelContent.addComponent(new Label("Created by: "));
        panelContent.addComponent(new Label(revision.getAuthor()));    // TODO: Map the authors to actual users
        if (StringUtils.isNotBlank(revision.getReviewer())) {
            panelContent.addComponent(new Label("Reviewed by: "));
            panelContent.addComponent(new Label(revision.getReviewer()));
        }
        panelContent.addComponent(new Label("Created at: "));
        panelContent.addComponent(new Label(revision.getCommitTime().toString()));

        return panelContent;
    }

    protected abstract String getViewName();

    abstract void buildContent(Layout mainPanel);

}
