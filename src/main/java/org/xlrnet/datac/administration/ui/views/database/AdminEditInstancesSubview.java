package org.xlrnet.datac.administration.ui.views.database;

import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.exception.IllegalUIStateException;
import org.xlrnet.datac.commons.ui.NotificationUtils;
import org.xlrnet.datac.database.domain.DeploymentGroup;
import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.database.domain.IDatabaseInstance;
import org.xlrnet.datac.database.services.DatabaseConnectionService;
import org.xlrnet.datac.database.services.DatabaseDeploymentManagementService;
import org.xlrnet.datac.database.util.DatabaseGroupHierarchicalDataProvider;
import org.xlrnet.datac.database.util.DatabaseInstanceIconProvider;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.session.ui.views.AbstractSubview;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.services.BranchService;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.renderers.HtmlRenderer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Subview for editing the instances bound to a project.
 */
@Slf4j
@SpringComponent
@SpringView(name = AdminEditInstancesSubview.VIEW_NAME)
public class AdminEditInstancesSubview extends AbstractSubview {

    public static final String VIEW_NAME = "admin/instances";

    /** Service for accessing project data. */
    private final ProjectService projectService;

    /** Service for accessing instance data. */
    private final DatabaseDeploymentManagementService deploymentManagementService;

    /** Service for accessing connection data. */
    private final DatabaseConnectionService connectionService;

    /** Service for accessing branch data. */
    private final BranchService branchService;

    /** Group selected for editing. */
    private DeploymentGroup selectedGroup;

    /** The project which is being edited. */
    @Getter(AccessLevel.PROTECTED)
    private Project project;

    /** Form for editing groups. */
    private final AdminDeploymentGroupForm groupForm;

    /** Form for editing instances. */
    private final AdminDeploymentInstanceForm instanceForm;

    /** Provider for hierarchical data. */
    private DatabaseGroupHierarchicalDataProvider dataProvider;

    /** Data grid. */
    private TreeGrid<IDatabaseInstance> treeGrid;

    /** Instance selected for editing. */
    private DeploymentInstance selectedInstance;

    /** List of branches which may be selected. */
    private List<Branch> availableBranchesInProject;

    @Autowired
    public AdminEditInstancesSubview(ProjectService projectService, DatabaseDeploymentManagementService deploymentManagementService, DatabaseConnectionService connectionService, BranchService branchService, AdminDeploymentGroupForm groupForm, AdminDeploymentInstanceForm instanceForm) {
        this.projectService = projectService;
        this.deploymentManagementService = deploymentManagementService;
        this.connectionService = connectionService;
        this.branchService = branchService;
        this.groupForm = groupForm;
        this.instanceForm = instanceForm;
    }

    @Override
    protected void initialize() {
        Long projectId = null;
        if (getParameters().length == 1 && NumberUtils.isDigits(getParameters()[0])) {
            projectId = Long.valueOf(getParameters()[0]);
            project = projectService.findOne(projectId);
        }
        if (project == null) {
            LOGGER.warn("Unable to find project {}", project);
            throw new IllegalUIStateException("Unable to find project " + projectId, VIEW_NAME, getParameters());
        }
        availableBranchesInProject = branchService.findAllWatchedByProject(project);
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        return String.format("Manage the database instances for the project %s. You can group instances e.g. by stages " +
                        "or locations. Properties are inherited from the parent group, unless explicitly defined.",
                project.getName());
    }

    @NotNull
    @Override
    protected String getTitle() {
        return String.format("Instance configuration of %s", project.getName());
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        MHorizontalLayout mainLayout = new MHorizontalLayout().withFullSize();

        DatabaseGroupHierarchicalDataProvider groupSelectorDataProvider = new DatabaseGroupHierarchicalDataProvider(
                deploymentManagementService, getProject(), true);
        DeploymentGroupSelectorWindow groupSelectorWindow = new DeploymentGroupSelectorWindow(groupSelectorDataProvider);

        MButton newGroupButton = new MButton("New group");
        newGroupButton.setIcon(VaadinIcons.PLUS);
        newGroupButton.addClickListener(e -> {
            hideForms();
            groupSelectorWindow.setAllowRootSelection(true);
            groupSelectorWindow.setSuccessHandler(this::newParentGroup);
            getUI().addWindow(groupSelectorWindow);
        });
        MButton newInstanceButton = new MButton("New instance");
        newInstanceButton.setIcon(VaadinIcons.PLUS);
        newInstanceButton.addClickListener(e -> {
            hideForms();
            groupSelectorWindow.setAllowRootSelection(false);
            groupSelectorWindow.setSuccessHandler(this::newInstance);
            getUI().addWindow(groupSelectorWindow);
        });
        MHorizontalLayout buttonLayout = new MHorizontalLayout().withMargin(false);
        buttonLayout.with(newGroupButton);
        buttonLayout.with(newInstanceButton);
        MVerticalLayout editorLayout = new MVerticalLayout().withMargin(false).withStyleName("editor-list-form");
        editorLayout.with(buttonLayout);
        editorLayout.with(groupForm);
        editorLayout.with(instanceForm);

        dataProvider = new DatabaseGroupHierarchicalDataProvider(deploymentManagementService, getProject(), true);
        treeGrid = new TreeGrid<>(dataProvider);
        treeGrid.setWidth("100%");
        treeGrid.addSelectionListener((e) -> {
            if (e.getFirstSelectedItem().isPresent()) {
                IDatabaseInstance databaseInstance = e.getFirstSelectedItem().get();
                if (databaseInstance instanceof DeploymentGroup) {
                    selectedGroup = (DeploymentGroup) databaseInstance;
                    groupForm.setVisible(true);
                    groupForm.setEntity(selectedGroup);
                    instanceForm.setVisible(false);
                } else if (databaseInstance instanceof DeploymentInstance) {
                    selectedInstance = (DeploymentInstance) databaseInstance;
                    groupForm.setVisible(false);
                    instanceForm.setVisible(true);
                    instanceForm.setEntity(selectedInstance);
                }
            } else {
                selectedGroup = null;
                selectedInstance = null;
                groupForm.setVisible(false);
                instanceForm.setVisible(false);
            }
        });
        treeGrid.addColumn(new DatabaseInstanceIconProvider(), new HtmlRenderer()).setCaption("Name");
        groupForm.setSavedHandler(this::saveGroup);
        groupForm.setDeleteHandler(this::deleteGroup);
        groupForm.setDeleteMessageGenerator(this::generateDeleteMessage);
        groupForm.setResetHandler(g -> groupForm.setVisible(false));
        groupForm.setAvailableBranches(availableBranchesInProject);
        groupForm.setVisible(false);
        instanceForm.setVisible(false);
        instanceForm.setAvailableBranches(availableBranchesInProject);
        instanceForm.setResetHandler(i -> instanceForm.setVisible(false));
        // TODO: setup instance handlers
        treeGrid.expand(dataProvider.getDeploymentRoot());

        mainLayout.with(treeGrid).withExpand(treeGrid, 0.75f);
        mainLayout.with(editorLayout).withExpand(editorLayout, 0.25f);

        return mainLayout;
    }

    private void hideForms() {
        groupForm.setVisible(false);
        instanceForm.setVisible(false);
    }

    private void newInstance(DeploymentGroup parentGroup) {
        DeploymentInstance deploymentInstance = new DeploymentInstance(parentGroup);
        instanceForm.setAvailableConnections(connectionService.findAllWithoutInstanceOrderByNameAsc());
        instanceForm.setEntity(deploymentInstance);
        instanceForm.setVisible(true);
    }

    private void newParentGroup(DeploymentGroup parentGroup) {
        DeploymentGroup deploymentGroup = new DeploymentGroup(project, parentGroup);
        groupForm.setEntity(deploymentGroup);
        groupForm.setVisible(true);
    }

    @NotNull
    private String generateDeleteMessage(DeploymentGroup deploymentGroup) {
        return String.format("Do you really want to delete the group%n%s?%nThis removes all historic deployment information and can't be reverted!", deploymentGroup.getName());
    }

    private void deleteGroup(DeploymentGroup deploymentGroup) {
        if (deploymentManagementService.hasChildGroups(deploymentGroup) || !deploymentGroup.getInstances().isEmpty()) {
            NotificationUtils.showError("Deployment groups with children can't be deleted.", false);
            return;
        }
        deploymentManagementService.delete(deploymentGroup);
        groupForm.setVisible(false);
        dataProvider.refreshAll();
    }

    private void saveGroup(DeploymentGroup deploymentGroup) {
        deploymentManagementService.save(deploymentGroup);
        if (deploymentGroup.getParent() != null) {
            dataProvider.refreshItem(deploymentGroup.getParent());
            treeGrid.expand(deploymentGroup.getParent());
        } else {
            dataProvider.refreshAll();
        }
        groupForm.setVisible(false);
    }
}
