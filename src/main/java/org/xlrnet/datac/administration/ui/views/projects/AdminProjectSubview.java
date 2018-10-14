package org.xlrnet.datac.administration.ui.views.projects;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;
import org.vaadin.spring.events.annotation.EventBusListenerTopic;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.grid.MGrid;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.administration.services.ApplicationMaintenanceService;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.ui.NotificationUtils;
import org.xlrnet.datac.commons.ui.TemporalRenderer;
import org.xlrnet.datac.database.services.ChangeSetService;
import org.xlrnet.datac.foundation.EventTopics;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.ProjectState;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.foundation.services.ProjectUpdateEvent;
import org.xlrnet.datac.foundation.ui.services.NavigationService;
import org.xlrnet.datac.session.ui.views.AbstractSubview;
import org.xlrnet.datac.vcs.services.LockingService;
import org.xlrnet.datac.vcs.services.ProjectSchedulingService;
import org.xlrnet.datac.vcs.services.ProjectUpdateStarter;

import com.vaadin.data.ValueProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.TextRenderer;
import com.vaadin.ui.themes.ValoTheme;

import de.steinwedel.messagebox.MessageBox;
import elemental.json.JsonValue;

/**
 * Admin view for managing projects responsible for managing the available users.
 */
@ViewScope
@SpringComponent
@SpringView(name = AdminProjectSubview.VIEW_NAME)
public class AdminProjectSubview extends AbstractSubview {

    public static final String VIEW_NAME = "admin/projects";

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminProjectSubview.class);

    /**
     * Service for accessing transactional project data.
     */
    private final ProjectService projectService;

    /**
     * The update service for projects.
     */
    private final ProjectUpdateStarter projectUpdateStarter;

    /**
     * The grid component containing the projects.
     */
    private MGrid<Project> grid = new MGrid<>();

    /**
     * Central locking service.
     */
    private final LockingService lockingService;

    /**
     * Read-only form of the project.
     */
    private final ReadOnlyProjectInfoForm readOnlyProjectInfoForm;

    /** Service for navigation to other views. */
    private final NavigationService navigationService;

    /** Service for accessing change sets. */
    private final ChangeSetService changeSetService;

    /** Map with current project update progress. */
    private static ConcurrentMap<Long, Double> PROGRESS_MAP = new ConcurrentHashMap<>();

    private MButton newButton = new MButton("New project").withIcon(VaadinIcons.PLUS);

    private MButton editButton = new MButton("Edit").withIcon(VaadinIcons.PENCIL).withVisible(false);

    @Autowired
    public AdminProjectSubview(EventBus.ApplicationEventBus applicationEventBus, ApplicationMaintenanceService maintenanceService, ProjectService projectService, ProjectUpdateStarter projectUpdateStarter, ProjectSchedulingService projectSchedulingService, LockingService lockingService, ReadOnlyProjectInfoForm readOnlyProjectInfoForm, NavigationService navigationService, ChangeSetService changeSetService) {
        super(applicationEventBus, maintenanceService);
        this.projectService = projectService;
        this.projectUpdateStarter = projectUpdateStarter;
        this.lockingService = lockingService;
        this.readOnlyProjectInfoForm = readOnlyProjectInfoForm;
        this.navigationService = navigationService;
        this.changeSetService = changeSetService;
    }

    @Override
    protected void initialize() {
        // No initialization necessary
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        MHorizontalLayout mainLayout = new MHorizontalLayout().withFullSize();
        buildGrid();

        MHorizontalLayout buttonLayout = new MHorizontalLayout().withMargin(false);

        newButton.addClickListener((e) -> UI.getCurrent().getNavigator().navigateTo(AdminEditProjectSubview.VIEW_NAME + "/new"));
        editButton.addClickListener(e -> editProject(readOnlyProjectInfoForm.getEntity()));
        buttonLayout.with(newButton, editButton);
        readOnlyProjectInfoForm.setVisible(false);

        MVerticalLayout editorLayout = new MVerticalLayout().withMargin(false);
        editorLayout.with(buttonLayout);
        editorLayout.with(readOnlyProjectInfoForm);

        mainLayout.with(grid).withExpand(grid, 0.75f);
        mainLayout.with(editorLayout).withExpand(editorLayout, 0.25f);

        return mainLayout;
    }

    private void buildGrid() {
        grid.withFullSize();
        grid.addColumn(ValueProvider.identity(), new ProjectStateRenderer())
                .setCaption("State").setWidth(200);
        grid.addColumn(Project::getName).setCaption("Name");
        grid.addColumn(Project::getUrl).setCaption("VCS Url").setMaximumWidth(514);
        grid.addColumn(Project::getLastChangeCheck, new TemporalRenderer()).setCaption("Last check for changes");
        grid.addComponentColumn(this::buildProjectActionComponent);

        // Select the project in the read-only form when clicked
        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                readOnlyProjectInfoForm.setEntity(projectService.refresh(e.getValue()));
                readOnlyProjectInfoForm.setVisible(true);
                editButton.setVisible(true);
            } else {
                readOnlyProjectInfoForm.setVisible(false);
                editButton.setVisible(false);
            }
        });

        reloadProjects();
    }

    private MenuBar buildProjectActionComponent(Project project) {
        MenuBar menuBar = new MenuBar();
        menuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
        MenuBar.MenuItem actions = menuBar.addItem("Actions", null);
        actions.addItem("Update now", VaadinIcons.REFRESH, (x) -> forceUpdate(project));
        actions.addItem("Edit project", VaadinIcons.PENCIL, (x) -> editProject(project));
        actions.addItem("Configure instances", VaadinIcons.PACKAGE, (x) -> configureInstances(project));
        MenuBar.MenuItem tools = actions.addItem("Tools", VaadinIcons.TOOLBOX, null);
        actions.addSeparator();
        actions.addItem("Delete Project", VaadinIcons.TRASH, (x) -> deleteProject(project));
        tools.addItem("Reinitialize repository", (x) -> reinitRepository(project));
        tools.addItem("Reset indexed changes", (x) -> resetIndexedChanges(project));
        return menuBar;
    }

    private void resetIndexedChanges(Project project) {
        MessageBox.createWarning()
                .withCaption("Resetting change sets")
                .withHtmlMessage("Resetting the change sets will make you lose <b>all</b> indexed changes!<br/><br/>" +
                        "Use this only if you know what you do!<br>Do you want to continue?")
                .withYesButton(() -> {
                try {
                    changeSetService.resetChanges(project);
                    NotificationUtils.showSuccess("Changes reset successfully ");
                } catch (DatacTechnicalException | RuntimeException ex) {
                    LOGGER.error("Resetting changes in project {} failed", project.getName(), ex);
                    NotificationUtils.showError("Resetting changes failed", ex.getMessage(), false);
                }})
                .withNoButton()
                .open();
    }

    private void reinitRepository(Project project) {
        MessageBox.createQuestion()
                .withMessage("The repository will be reinitialized on the next update. Are you sure?")
                .withYesButton(() -> {
                    Project refresh = projectService.refresh(project);
                    boolean lock = lockingService.tryLock(refresh);
                    if (lock) {
                        try {
                            refresh.setInitialized(false);
                            projectService.save(refresh);
                        } finally {
                            lockingService.unlock(refresh);
                        }
                    } else {
                        NotificationUtils.showError("Project locked.", false);
                    }
                })
                .withNoButton()
                .open();
    }

    private void configureInstances(Project project) {
        navigationService.openConfigureInstancesView(project);
    }

    private void editProject(Project project) {
        navigationService.openEditProjectView(project);
    }

    @EventBusListenerMethod
    @EventBusListenerTopic(topic = EventTopics.PROJECT_UPDATE)
    private void handeProjectStateUpdate(ProjectUpdateEvent event) {
        PROGRESS_MAP.put(event.getProject().getId(), event.getProgress());
        runOnUiThread(() -> grid.getDataProvider().refreshItem(event.getProject()));
    }

    private void reloadProjects() {
        grid.setItems(projectService.findAllAlphabetically());
    }

    private void deleteProject(Project project) {
        MessageBox.createWarning()
                .withCaption("Delete project")
                .withHtmlMessage(String.format("Do you want to delete the project %s?<br>This action cannot be reverted!", StringEscapeUtils.escapeHtml4(project.getName())))
                .withYesButton(() -> {
                    if (lockingService.tryLock(project)) {
                        try {
                            readOnlyProjectInfoForm.setVisible(false);
                            projectService.deleteClean(project);
                            NotificationUtils.showSuccess("Project deleted successfully");
                            reloadProjects();
                        } catch (DatacTechnicalException e) {
                            LOGGER.error("Deleting project failed", e);
                            NotificationUtils.showError("Deleting project failed", true);
                        } finally {
                            lockingService.unlock(project);
                        }
                    } else {
                        NotificationUtils.showError("Project is locked", false);
                    }
                })
                .withNoButton()
                .open();
    }

    private void forceUpdate(Project item) {
        if (projectUpdateStarter.queueProjectUpdate(item)) {
            NotificationUtils.showSuccess("Project update queued");
        } else {
            NotificationUtils.showWarning("Project is locked");
        }
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        return "Projects are used to manage a specific database configuration backed by a version control system (VCS).";
    }

    @NotNull
    @Override
    protected String getTitle() {
        return "Project administration";
    }

    private class ProjectStateRenderer extends TextRenderer {

        @Override
        public JsonValue encode(Object value) {
            Project project = (Project) value;
            ProjectState projectState = project.getState();
            String renderText = projectState.isProgressable() ? String.format("%s (%.0f %%)", projectState.toString(), PROGRESS_MAP.get(project.getId())) : projectState.toString();
            return super.encode(renderText);
        }
    }

}
