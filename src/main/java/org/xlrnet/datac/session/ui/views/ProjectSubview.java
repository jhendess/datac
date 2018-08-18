package org.xlrnet.datac.session.ui.views;

import com.vaadin.data.HasValue;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.vaadin.spring.events.EventBus;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.administration.services.ApplicationMaintenanceService;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.IllegalUIStateException;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.ui.services.NavigationService;
import org.xlrnet.datac.session.ui.components.project.AbstractProjectLayout;
import org.xlrnet.datac.session.ui.components.project.ProjectChangeLayout;
import org.xlrnet.datac.session.ui.components.project.ProjectRevisionLayout;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.BranchService;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

import java.util.Collections;
import java.util.List;

import static org.xlrnet.datac.session.ui.views.ProjectSubview.VIEW_NAME;

/**
 * Main project view which provides a tabbed view on a single proejct.
 */
@SpringComponent
@Scope("prototype")
@SpringView(name = VIEW_NAME)
public class ProjectSubview extends AbstractSubview {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectSubview.class);

    public static final String VIEW_NAME = "project";

    private static final String DEFAULT_TAB = "changes";

    /**
     * Number of revisions to traverse in order to find database changes.
     */
    static final int REVISIONS_TO_TRAVERSE = 200;

    /**
     * Parameter which may contain a branch name.
     */
    private static final String BRANCH_PARAMETER = "branch";
    public static final String EMPTY_BRANCH_CAPTION = "<Select a branch>";

    /**
     * Service for accessing branch data.
     */
    private final BranchService branchService;

    /**
     * Service for navigating to various view states.
     */
    private final NavigationService navigationService;

    /**
     * Service for accessing revision graph.
     */
    private final RevisionGraphService revisionGraphService;

    /**
     * The current project.
     */
    private Project project;

    /**
     * The current branch.
     */
    private Branch branch;

    /**
     * The revision which is effectively displayed.
     */
    private Revision revision;

    /**
     * The tab for displaying changes in a project.
     */
    private final ProjectChangeLayout changeTab;

    /**
     * The tab for displaying revisions in a project.
     */
    private final ProjectRevisionLayout revisionTab;

    private TabSheet tabSheet;

    @Autowired
    public ProjectSubview(EventBus.ApplicationEventBus applicationEventBus, ApplicationMaintenanceService maintenanceService, BranchService branchService, NavigationService navigationService, RevisionGraphService revisionGraphService, ProjectChangeLayout changeTab, ProjectRevisionLayout revisionTab) {
        super(applicationEventBus, maintenanceService);
        this.branchService = branchService;
        this.navigationService = navigationService;
        this.revisionGraphService = revisionGraphService;
        this.changeTab = changeTab;
        this.revisionTab = revisionTab;
    }

    @Override
    protected void initialize() throws DatacTechnicalException {
        // TODO: both project, revision, branch as Tab as view parameters - e.g.: /<projectId>/VIEW_TYPE/<revisionId>?branch=<branchId>
        Long revisionId = null;
        if (getParameters().length == 1 && NumberUtils.isDigits(getParameters()[0])) {
            revisionId = Long.valueOf(getParameters()[0]);
            revision = revisionGraphService.findOne(revisionId);
        }
        if (revision == null) {
            LOGGER.warn("Unable to find revision {}", revisionId);
            throw new IllegalUIStateException("Unable to find revision " + revisionId, VIEW_NAME, getParameters());
        } else {
            project = revision.getProject();
        }

        // Only operate on the cached revision
        revision = revisionGraphService.findCachedByInternalIdAndProject(revision.getInternalId(), project);

        String branchParameter = getNamedParameter(BRANCH_PARAMETER);
        if (branchParameter != null) {
            branch = branchService.findOne(Long.valueOf(branchParameter));
        }
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        MVerticalLayout mainLayout = new MVerticalLayout();
        mainLayout.add(buildNavigationPanel());
        tabSheet = new TabSheet();
        configureTabs(tabSheet);
        tabSheet.setStyleName(ValoTheme.TREETABLE_BORDERLESS);
        /* Install tab change listener. */
        tabSheet.addSelectedTabChangeListener(e -> {
            AbstractProjectLayout selectedTab = (AbstractProjectLayout) e.getTabSheet().getSelectedTab();
            selectedTab.beforeContentRefresh();
            selectedTab.setActiveRevisionAndRefreshContent(revision, branch);
            updateTitle(selectedTab.getTitle());
            updateSubtitle(selectedTab.getSubtitle());
        });
        if (revision != null) {
            /* TODO: Switch to the initial tab. */
            tabSheet.setSelectedTab(revisionTab);
            mainLayout.add(tabSheet);
        }

        return mainLayout;
    }

    private Component buildNavigationPanel() {
        MHorizontalLayout navigationPanel = new MHorizontalLayout();

        NativeSelect<Branch> branchSelector = new NativeSelect<>();
        List<Branch> branchList = branchService.findAllWatchedByProject(project);
        if (branch != null && !branchList.contains(branch)) {
            branchList.add(branch);
        }
        Collections.sort(branchList);
        branchSelector.setItems(branchList);
        branchSelector.setEmptySelectionAllowed(false);
        //branchSelector.setEmptySelectionCaption(EMPTY_BRANCH_CAPTION);
        branchSelector.setSelectedItem(branch);
        branchSelector.addValueChangeListener(this::handleBranchSelectionChange);
        branchSelector.setItemCaptionGenerator(Branch::getName);
        branchSelector.setCaption("Change branch");
        branchSelector.setIcon(VaadinIcons.ROAD_BRANCH);
        navigationPanel.add(branchSelector);

        Button editProjectButton = new Button("Edit project");
        editProjectButton.addClickListener(e -> navigationService.openEditProjectView(project));
        navigationPanel.add(editProjectButton);

        return navigationPanel.alignAll(Alignment.BOTTOM_LEFT);
    }

    private void handleBranchSelectionChange(HasValue.ValueChangeEvent<Branch> branchValueChangeEvent) {
        Branch newBranch = branchValueChangeEvent.getValue();
        if (newBranch != null) {
            Revision newRevision = revisionGraphService.findLastRevisionOnBranch(newBranch);
            if (newRevision != null) {
                newRevision = revisionGraphService.findCachedByInternalIdAndProject(newRevision.getInternalId(), newRevision.getProject());
            }
            changeRevision(newRevision, newBranch);
        }
    }

    private void changeRevision(Revision newRevision, Branch newBranch) {
        this.revision = newRevision;
        this.branch = newBranch;
        AbstractProjectLayout selectedTab = (AbstractProjectLayout) tabSheet.getSelectedTab();
        selectedTab.setActiveRevisionAndRefreshContent(revision, newBranch);
        updateTitle(selectedTab.getTitle());
        updateSubtitle(selectedTab.getSubtitle());
    }

    private void configureTabs(TabSheet tabSheet) {
        changeTab.setProject(project);
        tabSheet.addTab(changeTab, "Changes");
        revisionTab.setProject(project);
        tabSheet.addTab(revisionTab, "Revisions");
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        if (revision == null) {
            return "There is no revision in this project. Make sure that you ran a project update first.";
        }
        return project.getName();
    }

    @NotNull
    @Override
    protected String getTitle() {
        return project.getName();
    }
}
