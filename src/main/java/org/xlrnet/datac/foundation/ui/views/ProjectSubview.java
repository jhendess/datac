package org.xlrnet.datac.foundation.ui.views;

import com.vaadin.server.ExternalResource;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.graph.BreadthFirstTraverser;
import org.xlrnet.datac.commons.util.DateTimeUtils;
import org.xlrnet.datac.database.domain.DatabaseChange;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.services.ChangeSetService;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.vcs.domain.Revision;
import org.xlrnet.datac.vcs.services.RevisionGraphService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Overview for projects.
 */
@SpringComponent
@SpringView(name = ProjectSubview.VIEW_NAME)
public class ProjectSubview extends AbstractSubview {

    public static final String VIEW_NAME = "projects";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectSubview.class);

    private static final int MAX_BEFORE_TRUNCATE = 80;

    private static final int MAX_REVISIONS_TO_VISIT = 50;
    public static final String NEWLINE = "\n";

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
     * Helper class for performing breadth first traversals on revision graphs.
     */
    private final BreadthFirstTraverser<Revision> breadthFirstTraverser = new BreadthFirstTraverser<>();

    @Autowired
    public ProjectSubview(ProjectService projectService, ChangeSetService changeSetService, RevisionGraphService revisionGraphService) {
        this.projectService = projectService;
        this.changeSetService = changeSetService;
        this.revisionGraphService = revisionGraphService;
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
        Iterable<Project> projects = projectService.findAll();

        for (Project project : projects) {
            Component projectPanel = createPanelForProject(project);
            layout.addComponent(projectPanel);
        }

        return layout;
    }

    private Component createPanelForProject(Project project) {
        Panel panel = new Panel(project.getName());
        panel.setWidth("75%");

        GridLayout panelContent = new GridLayout(2, 5);
        panelContent.setStyleName("projectPanel");

        if (StringUtils.isNotBlank(project.getDescription())) {
            panelContent.addComponent(new Label("Description: "));
            Label descriptionLabel = new Label(project.getDescription());
            panelContent.addComponent(descriptionLabel);
        }
        if (StringUtils.isNotBlank(project.getWebsite())) {
            panelContent.addComponent(new Label("Website: "));
            Link websiteLink = new Link(project.getWebsite(), new ExternalResource(project.getWebsite()));
            panelContent.addComponent(websiteLink);
        }

        panelContent.addComponent(new Label("Update status: "));
        panelContent.addComponent(new Label(project.getState().toString()));        // TODO: Auto-update the state using the event bus

        panelContent.addComponent(new Label("Last update: "));
        panelContent.addComponent(new Label(project.getLastChangeCheck() != null ? DateTimeUtils.format(project.getLastChangeCheck()) : "Never"));

        panelContent.addComponent(new Label("Latest dev revisions: "));
        panelContent.addComponent(buildLastRevisionsLayout(project));

        panelContent.addComponent(new Label("Latest dev change sets: "));
        panelContent.addComponent(buildLastChangesLayout(project));

        panel.setContent(panelContent);
        return panel;
    }

    private Component buildLastChangesLayout(Project project) {
        //changeSetService.findLastChangeSetsInProject(project);
        Layout layout = new GridLayout(1, 3);
        layout.setStyleName("listLayout");
        Revision lastDevRevision = revisionGraphService.findByInternalIdAndProject(project.getDevelopmentBranch().getInternalId(), project);
        List<DatabaseChangeSet> changeSets = new ArrayList<>();
        AtomicInteger visitedRevisions = new AtomicInteger(0);

        try {
            breadthFirstTraverser.traverseParentsCutOnMatch(lastDevRevision, (r) -> {
                if (changeSetService.countByRevision(r) > 0) {
                    List<DatabaseChangeSet> changeSetsInRevision = changeSetService.findAllInRevision(r);
                    for (int i = changeSetsInRevision.size() - 1, k = 0; i > 0 && k < 3; i--) {
                        changeSets.add(changeSetsInRevision.get(i));
                        k++;
                    }
                }
            }, (r -> (visitedRevisions.incrementAndGet() > MAX_REVISIONS_TO_VISIT || changeSets.size() == 3)));
        } catch (DatacTechnicalException e) {
            Label label = new Label("Unexpected error while loading last changesets");
            label.setStyleName(ValoTheme.LABEL_FAILURE);
            layout.addComponent(label);
            LOGGER.error("Unexpected error while loading last changesets in project {}", project.getName(), e);
        }

        if (!changeSets.isEmpty()) {
            for (DatabaseChangeSet changeSet : changeSets) {
                String message = changeSet.getComment();
                if (message != null && message.length() > MAX_BEFORE_TRUNCATE) {
                    message = StringUtils.truncate(message, MAX_BEFORE_TRUNCATE) + "...";
                }
                if (StringUtils.isBlank(message)) {
                    if (!changeSet.getChanges().isEmpty()) {
                        DatabaseChange firstChange = changeSet.getChanges().get(0);
                        if (StringUtils.isNotBlank(firstChange.getPreviewSql())) {
                            message = firstChange.getPreviewSql();
                            if (message.length() > MAX_BEFORE_TRUNCATE) {
                                message = StringUtils.truncate(message, MAX_BEFORE_TRUNCATE) + "...";
                            }
                        } else {
                            message = firstChange.getType();
                        }
                    } else {
                        message = changeSet.getChecksum();
                    }
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
        Layout layout = new GridLayout(1, 3);
        layout.setStyleName("listLayout");
        Revision lastDevRevision = revisionGraphService.findByInternalIdAndProject(project.getDevelopmentBranch().getInternalId(), project);
        List<Revision> revisions = new ArrayList<>();
        try {
            breadthFirstTraverser.traverseParentsCutOnMatch(lastDevRevision, revisions::add, (r -> revisions.size() >= 3));
        } catch (DatacTechnicalException e) {
            Label label = new Label("Unexpected error while loading revisions");
            label.setStyleName(ValoTheme.LABEL_FAILURE);
            layout.addComponent(label);
            LOGGER.error("Unexpected error while loading last revisions in project {}", project.getName(), e);
        }

        if (!revisions.isEmpty()) {
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
