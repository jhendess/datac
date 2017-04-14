package org.xlrnet.datac.administration.ui.views.projects;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.*;
import com.vaadin.ui.renderers.ButtonRenderer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;
import org.vaadin.spring.events.annotation.EventBusListenerTopic;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.ui.NotificationUtils;
import org.xlrnet.datac.foundation.EventTopics;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.foundation.services.ProjectUpdateEvent;
import org.xlrnet.datac.foundation.ui.components.SimpleOkCancelWindow;
import org.xlrnet.datac.foundation.ui.views.AbstractSubview;
import org.xlrnet.datac.vcs.services.LockingService;
import org.xlrnet.datac.vcs.services.ProjectUpdateStarter;

import javax.annotation.PostConstruct;
import java.util.Collection;

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
     * Event bus for application scoped events.
     */
    private final EventBus.ApplicationEventBus applicationEventBus;

    /**
     * The project service for accessing projects.
     */
    private final ProjectService projectService;

    /**
     * The update service for projects.
     */
    private final ProjectUpdateStarter projectUpdateStarter;

    /**
     * Button for new projects.
     */
    private Button newButton;

    /**
     * Main layout.
     */
    private VerticalLayout layout;

    /**
     * The grid component containing the projects.
     */
    private Grid<Project> grid = new Grid<>();

    /**
     * Central locking service.
     */
    private final LockingService lockingService;

    private VaadinSession vaadinSession;

    @Autowired
    public AdminProjectSubview(EventBus.ApplicationEventBus viewEventBus, ProjectService projectService, ProjectUpdateStarter projectUpdateStarter, LockingService lockingService) {
        this.applicationEventBus = viewEventBus;
        this.projectService = projectService;
        this.projectUpdateStarter = projectUpdateStarter;
        this.lockingService = lockingService;
    }

    @PostConstruct
    public void init() {
        vaadinSession = VaadinSession.getCurrent();
        applicationEventBus.subscribe(this);
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        layout = new VerticalLayout();

        newButton = new Button("New project");
        newButton.setIcon(VaadinIcons.PLUS);
        newButton.addClickListener((e) -> UI.getCurrent().getNavigator().navigateTo(AdminEditProjectSubview.VIEW_NAME + "/new"));
        layout.addComponent(newButton);

        layout.addComponent(buildGrid());

        return layout;
    }

    private Component buildGrid() {
        grid.addColumn(Project::getState).setCaption("State").setMaximumWidth(150);
        grid.addColumn(Project::getName).setCaption("Name");
        grid.addColumn(Project::getUrl).setCaption("VCS Url");
        grid.addColumn(Project::getLastChangeCheck).setCaption("Last check for changes");

        grid.addColumn(project -> "Update", new ButtonRenderer<>(clickEvent -> {
            forceUpdate(clickEvent.getItem());
        }));
        grid.addColumn(project -> "Edit", new ButtonRenderer<>(clickEvent -> {
            UI.getCurrent().getNavigator().navigateTo(AdminEditProjectSubview.VIEW_NAME + "/" + clickEvent.getItem().getId());
        }));
        grid.addColumn(project -> "Delete", new ButtonRenderer<>(clickEvent -> {
            deleteProject(clickEvent.getItem());
        }));

        grid.setWidth("80%");

        reloadProjects();
        return grid;
    }

    @EventBusListenerMethod
    @EventBusListenerTopic(topic = EventTopics.PROJECT_UPDATE)
    private void handeProjectStateUpdate(ProjectUpdateEvent event) {
        vaadinSession.access(() -> grid.getDataProvider().refreshItem(event.getProject()));
    }

    private void reloadProjects() {
        grid.setItems((Collection<Project>) projectService.findAll());
    }

    private void deleteProject(Project item) {
        SimpleOkCancelWindow window = new SimpleOkCancelWindow("Delete project");
        window.setCustomContent(new Label("Do you want to delete the project " + item.getName() + "?<br>This action cannot be reverted!", ContentMode.HTML));
        window.setOkHandler(() -> {
            if (lockingService.tryLock(item)) {
                try {
                    projectService.deleteClean(item);
                    NotificationUtils.showSuccess("Project deleted successfully");
                    reloadProjects();
                } catch (DatacTechnicalException e) {
                    LOGGER.error("Deleting project failed", e);
                    NotificationUtils.showError("Deleting project failed", true);
                } finally {
                    lockingService.unlock(item);
                }
            } else {
                NotificationUtils.showError("Project is locked", false);
            }
            window.close();
        });
        UI.getCurrent().addWindow(window);
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
}
