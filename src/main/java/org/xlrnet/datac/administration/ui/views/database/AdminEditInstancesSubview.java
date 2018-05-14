package org.xlrnet.datac.administration.ui.views.database;

import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.exception.IllegalUIStateException;
import org.xlrnet.datac.commons.ui.NotificationUtils;
import org.xlrnet.datac.database.domain.DeploymentGroup;
import org.xlrnet.datac.database.domain.IDatabaseInstance;
import org.xlrnet.datac.database.services.DatabaseConnectionService;
import org.xlrnet.datac.database.services.DatabaseDeploymentManagementService;
import org.xlrnet.datac.database.util.DatabaseGroupHierarchicalDataProvider;
import org.xlrnet.datac.database.util.DatabaseInstanceIconProvider;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.session.ui.views.AbstractSubview;

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

    /** Group selected for editing. */
    private DeploymentGroup selectedGroup;

    /** The project which is being edited. */
    @Getter(AccessLevel.PROTECTED)
    private Project project;

    /** Form for editing groups. */
    private final AdminDeploymentGroupForm groupForm;

    /** Provider for hierarchical data. */
    private DatabaseGroupHierarchicalDataProvider dataProvider;

    private TreeGrid<IDatabaseInstance> treeGrid;

    @Autowired
    public AdminEditInstancesSubview(ProjectService projectService, DatabaseDeploymentManagementService deploymentManagementService, DatabaseConnectionService connectionService, AdminDeploymentGroupForm groupForm) {
        this.projectService = projectService;
        this.deploymentManagementService = deploymentManagementService;
        this.connectionService = connectionService;
        this.groupForm = groupForm;
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
        DeploymentGroupSelectorWindow groupSelectorWindow = new DeploymentGroupSelectorWindow(groupSelectorDataProvider, this::newParentGroup);

        MButton newGroupButton = new MButton("New group");
        newGroupButton.setIcon(VaadinIcons.PLUS);
        newGroupButton.addClickListener(e -> {
            groupForm.setVisible(false);
            getUI().addWindow(groupSelectorWindow);
        });
        MButton newInstanceButton = new MButton("New instance").withEnabled(false);
        newInstanceButton.setIcon(VaadinIcons.PLUS);
        newInstanceButton.addClickListener(e -> {
            // TODO
        });
        MHorizontalLayout buttonLayout = new MHorizontalLayout().withMargin(false);
        buttonLayout.with(newGroupButton);
        buttonLayout.with(newInstanceButton);
        MVerticalLayout editorLayout = new MVerticalLayout().withMargin(false).withStyleName("editor-list-form");
        editorLayout.with(buttonLayout);
        editorLayout.with(groupForm);

        dataProvider = new DatabaseGroupHierarchicalDataProvider(deploymentManagementService, getProject(), true);
        treeGrid = new TreeGrid<>(dataProvider);
        treeGrid.setWidth("100%");
        treeGrid.addSelectionListener((e) -> {
            if (e.getFirstSelectedItem().isPresent()) {
                if (e.getFirstSelectedItem().get() instanceof DeploymentGroup) {
                    selectedGroup = (DeploymentGroup) e.getFirstSelectedItem().get();
                    groupForm.setVisible(true);
                    groupForm.setEntity(selectedGroup);
                }
            } else {
                selectedGroup = null;
                groupForm.setVisible(false);
            }
        });
        treeGrid.addColumn(new DatabaseInstanceIconProvider(), new HtmlRenderer()).setCaption("Name");
        groupForm.setSavedHandler(this::saveGroup);
        groupForm.setDeleteHandler(this::deleteGroup);
        groupForm.setDeleteMessageGenerator(this::generateDeleteMessage);
        groupForm.setResetHandler(g -> groupForm.setVisible(false));
        groupForm.setVisible(false);
        treeGrid.expand(dataProvider.getDeploymentRoot());

        mainLayout.with(treeGrid).withExpand(treeGrid, 0.75f);
        mainLayout.with(editorLayout).withExpand(editorLayout, 0.25f);

        return mainLayout;
    }

    private void newParentGroup(DeploymentGroup parentGroup) {
        DeploymentGroup deploymentGroup = new DeploymentGroup(project, parentGroup);
        groupForm.setEntity(deploymentGroup);
        groupForm.setVisible(true);
    }

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
