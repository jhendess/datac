package org.xlrnet.datac.administration.ui.views.database;

import com.vaadin.data.ValueProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.TreeGrid;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.exception.IllegalUIStateException;
import org.xlrnet.datac.database.domain.DeploymentGroup;
import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.database.domain.IDatabaseInstance;
import org.xlrnet.datac.database.services.DatabaseConnectionService;
import org.xlrnet.datac.database.services.DatabaseDeploymentManagementService;
import org.xlrnet.datac.database.util.DeploymentGroupHierarchcialDataProvider;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.session.ui.views.AbstractSubview;

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

    /** Renderer for name incl. an type-specific icon. */
    private final ValueProvider<IDatabaseInstance, String> nameRenderer = (i) -> {
        String iconHtml = (i.isGroup() ? VaadinIcons.FOLDER : VaadinIcons.CONNECT).getHtml();
        return String.format("%s %s", iconHtml, StringEscapeUtils.escapeHtml4(i.getName()));
    };

    @Autowired
    public AdminEditInstancesSubview(ProjectService projectService, DatabaseDeploymentManagementService deploymentManagementService, DatabaseConnectionService connectionService) {
        this.projectService = projectService;
        this.deploymentManagementService = deploymentManagementService;
        this.connectionService = connectionService;
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        MHorizontalLayout mainLayout = new MHorizontalLayout().withFullSize();

        DeploymentGroupHierarchialDataProvider dataProvider = new DeploymentGroupHierarchialDataProvider(deploymentManagementService, getProject());

        MButton newGroupButton = new MButton("New group");
        newGroupButton.setIcon(VaadinIcons.PLUS);
        newGroupButton.addClickListener(e -> {
            // TODO
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

        TreeGrid<IDatabaseInstance> treeGrid = new TreeGrid<>(dataProvider);
        treeGrid.setWidth("100%");
        treeGrid.addSelectionListener((e) -> {
            // TODO: Store the last selected value somewhere
            if (!e.getFirstSelectedItem().isPresent()) {
                newGroupButton.withEnabled(true);
                newInstanceButton.withEnabled(false);   // Instances aren't allowed on root level
            } else if (e.getFirstSelectedItem().get() instanceof DeploymentInstance) {
                newGroupButton.withEnabled(false);
                newInstanceButton.withEnabled(false);
                selectedGroup = null;
            } else {
                newGroupButton.withEnabled(true);
                newInstanceButton.withEnabled(true);
                selectedGroup = (DeploymentGroup) e.getFirstSelectedItem().get();
            }
            // TODO: This mechanism is pretty broken -> provide a separate window where the user can select the parent
        });
        treeGrid.addColumn(nameRenderer).setCaption("Name");

        // TODO: build layout

        mainLayout.with(treeGrid).withExpand(treeGrid, 0.75f);
        mainLayout.with(editorLayout).withExpand(editorLayout, 0.25f);

        return mainLayout;
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
}
