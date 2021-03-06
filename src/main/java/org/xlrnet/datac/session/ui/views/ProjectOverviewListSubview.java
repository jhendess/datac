package org.xlrnet.datac.session.ui.views;

import com.vaadin.server.ExternalResource;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MPanel;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.administration.services.ApplicationMaintenanceService;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.ui.DatacTheme;
import org.xlrnet.datac.commons.util.DateTimeUtils;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.services.ChangeSetService;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.foundation.ui.services.NavigationService;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

import java.util.List;

/**
 * Overview for projects.
 */
@SpringComponent
@SpringView(name = ProjectOverviewListSubview.VIEW_NAME)
public class ProjectOverviewListSubview extends AbstractSubview {

    public static final String VIEW_NAME = "projects";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectOverviewListSubview.class);

    private static final int MAX_BEFORE_TRUNCATE = 80;

    private static final int MAX_REVISIONS_TO_VISIT = 50;

    private static final String NEWLINE = "\n";

    private static final int CHANGE_SETS_TO_DISPLAY = 3;

    private static final int REVISIONS_TO_DISPLAY = 3;

    /**
     * Service for accessing project data.
     */
    private final ProjectService projectService;

    /**
     * Service for accessing change sets.
     */
    private final ChangeSetService changeSetService;

    /**
     * Service for accessing the revision graph.
     */
    private final RevisionGraphService revisionGraphService;

    /**
     * Service for navigating across views.
     */
    private final NavigationService navigationService;

    @Autowired
    public ProjectOverviewListSubview(EventBus.ApplicationEventBus applicationEventBus, ApplicationMaintenanceService maintenanceService, ProjectService projectService, ChangeSetService changeSetService, RevisionGraphService revisionGraphService, NavigationService navigationService) {
        super(applicationEventBus, maintenanceService);
        this.projectService = projectService;
        this.changeSetService = changeSetService;
        this.revisionGraphService = revisionGraphService;
        this.navigationService = navigationService;
    }

    @NotNull
    @Override
    protected String getTitle() {
        return "Project overview";
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        return "Get an overview over your projects' latest changes and status. Select the project you want to manage.";
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        Layout layout = new VerticalLayout();
        Iterable<Project> projects = projectService.findAllAlphabetically();

        for (Project project : projects) {
            Component projectPanel = createPanelForProject(project);
            layout.addComponent(projectPanel);
        }

        return layout;
    }

    @Override
    protected void initialize() {
        // Nothing to do
    }

    private Component createPanelForProject(Project project) {
        MPanel panel = new MPanel(project.getName()).withFullWidth().withStyleName("card", "card-2");
        panel.addStyleName("project-overview-panel");
        panel.addClickListener((e) -> navigationService.openChangeView(project.getDevelopmentBranch()));

        MVerticalLayout panelContent = new MVerticalLayout().withMargin(false);

        GridLayout gridLayout = new GridLayout(2, 2);
        panelContent.add(gridLayout);

        if (StringUtils.isNotBlank(project.getDescription())) {
            gridLayout.addComponent(new Label("Description: "));
            Label descriptionLabel = new Label(project.getDescription());
            gridLayout.addComponent(descriptionLabel);
        }
        if (StringUtils.isNotBlank(project.getWebsite())) {
            gridLayout.addComponent(new Label("Website: "));
            Link websiteLink = new Link(project.getWebsite(), new ExternalResource(project.getWebsite()));
            gridLayout.addComponent(websiteLink);
        }

        gridLayout.addComponent(new Label("Update status: "));
        gridLayout.addComponent(new Label(project.getState().toString()));        // TODO: Auto-update the state using the event bus

        gridLayout.addComponent(new Label("Last update: "));
        gridLayout.addComponent(new Label(project.getLastChangeCheck() != null ? DateTimeUtils.format(project.getLastChangeCheck()) : "Never"));

        MHorizontalLayout latestDataLayout = new MHorizontalLayout().withFullWidth();
        latestDataLayout.add(
                new MVerticalLayout().withMargin(false)
                        .with(new Label("Latest dev revisions: "))
                        .with(buildLastRevisionsLayout(project)));

        latestDataLayout.addComponent(
                new MVerticalLayout().withMargin(false)
                        .with(new Label("Latest dev change sets: "))
                        .with(buildLastChangesLayout(project)));

        panelContent.with(latestDataLayout);

        panel.setContent(panelContent);
        return panel;
    }

    private Component buildLastChangesLayout(Project project) {
        //changeSetService.findLastChangeSetsInProject(project);
        Layout layout = new GridLayout(1, 3);
        layout.setStyleName(DatacTheme.LIST_LAYOUT);
        List<DatabaseChangeSet> changeSets = null;

        try {
            changeSets = changeSetService.findLastDatabaseChangeSetsOnBranch(project.getDevelopmentBranch(), CHANGE_SETS_TO_DISPLAY, MAX_REVISIONS_TO_VISIT);
        } catch (DatacTechnicalException e) {
            Label label = new Label("Unexpected error while loading last changesets");
            label.setStyleName(ValoTheme.LABEL_FAILURE);
            layout.addComponent(label);
            LOGGER.error("Unexpected error while loading last changesets in project {}", project.getName(), e);
        }

        if (changeSets != null && !changeSets.isEmpty()) {
            for (DatabaseChangeSet changeSet : changeSets) {
                String message = changeSetService.formatDatabaseChangeSetTitle(changeSet);
                if (message.length() > MAX_BEFORE_TRUNCATE) {
                    message = StringUtils.truncate(message, MAX_BEFORE_TRUNCATE) + "...";
                }
                layout.addComponent(new Label(message));
                // TODO: Add links to more information about the changesets
            }
        } else {
            layout.addComponent(new Label("No changesets found on dev branch"));
        }

        return layout;
    }

    private Component buildLastRevisionsLayout(Project project) {
        Layout layout = new GridLayout(1, REVISIONS_TO_DISPLAY);
        layout.setStyleName(DatacTheme.LIST_LAYOUT);

        List<Revision> revisions = null;
        try {
            revisions = revisionGraphService.findLastRevisionsOnBranch(project.getDevelopmentBranch(), REVISIONS_TO_DISPLAY);
        } catch (DatacTechnicalException e) {
            Label label = new Label("Unexpected error while loading revisions");
            label.setStyleName(ValoTheme.LABEL_FAILURE);
            layout.addComponent(label);
            LOGGER.error("Unexpected error while loading last revisions in project {}", project.getName(), e);
        }

        if (revisions != null && !revisions.isEmpty()) {
            for (int i = 0; i < revisions.size() && i < 3; i++) {
                Revision revision = revisions.get(i);
                String message = StringUtils.isNotBlank(revision.getMessage()) ? StringUtils.substringBefore(revision.getMessage(), NEWLINE) :
                        revision.getCommitTime() != null ?
                                DateTimeUtils.format(revision.getCommitTime()) : revision.getInternalId();
                if (message.length() > MAX_BEFORE_TRUNCATE) {
                    message = StringUtils.truncate(message, MAX_BEFORE_TRUNCATE) + "...";
                }
                layout.addComponent(new Label(message));
                // TODO: Add links to more information about the revisions
            }
        } else {
            layout.addComponent(new Label("No revisions found on dev branch"));
        }

        return layout;
    }
}
