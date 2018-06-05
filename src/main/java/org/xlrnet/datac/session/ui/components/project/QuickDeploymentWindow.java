package org.xlrnet.datac.session.ui.components.project;

import java.util.Set;

import org.vaadin.addons.ComboBoxMultiselect;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.label.MLabel;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.vaadin.viritin.layouts.MWindow;
import org.xlrnet.datac.commons.ui.DatacTheme;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.database.domain.QuickDeploymentResult;
import org.xlrnet.datac.database.services.DeploymentInstanceService;
import org.xlrnet.datac.database.services.DeploymentManagerService;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.ui.components.ProgressWindow;
import org.xlrnet.datac.foundation.ui.util.ProgressWindowChangeHandler;
import org.xlrnet.datac.vcs.domain.Revision;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@UIScope
@SpringComponent
public class QuickDeploymentWindow extends MWindow {

    /** The change set which will be deployed. */
    private DatabaseChangeSet changeSet;

    /** The revision from which the deployment should be performed. */
    private Revision revision;

    /** The current project. */
    private Project project;

    /** Service for loading available instances. */
    private final DeploymentInstanceService instanceService;

    /** Service to execute deployments. */
    private final DeploymentManagerService deploymentManagerService;

    /** Checkbox to select the target instances for the deployment. */
    private ComboBoxMultiselect<DeploymentInstance> targetInstances = new ComboBoxMultiselect<>("Target instances");

    /** Flag to show only compatible instances as possible targets. */
    boolean compatibleInstancesOnly = true;

    /** Button to perform the deployment. */
    private MButton deployButton = new MButton("Perform deployment").withStyleName(ValoTheme.BUTTON_PRIMARY).withListener(this::performDeployment);

    /** Button to cancel the deployment. */
    private MButton cancelButton = new MButton("Cancel").withListener((e) -> this.close());

    /** Header. */
    private MLabel headerLabel = new MLabel("Perform quick deployment").withStyleName(ValoTheme.LABEL_H2);

    /** Info label. */
    private MLabel infoLabel = new MLabel("Quick deployments allow to you to execute a single change on multiple instances quickly. This mode is only recommended for quick and uncritical deployments (e.g. for test purposes).").withStyleName(DatacTheme.BREAKING_LABEL);

    /** Label in case of no available instances. */
    private MLabel noInstancesLabel = new MLabel("There are no compatible or writable deployment instances for this change. Either create new instances or change their tracking branch.")
            .withStyleName(ValoTheme.LABEL_FAILURE).withVisible(false);

    /** Window to display deployment progress. */
    private final ProgressWindow progressWindow = new ProgressWindow();

    /** Handler to update the progress window. */
    private final ProgressWindowChangeHandler progressWindowChangeHandler = new ProgressWindowChangeHandler(progressWindow);

    public QuickDeploymentWindow(DeploymentInstanceService instanceService, DeploymentManagerService deploymentManagerService) {
        this.instanceService = instanceService;
        this.deploymentManagerService = deploymentManagerService;
        setModal(true);
        setWidth("1200px");
        setHeight("600px");
        center();
        setContent(buildContent());

        targetInstances.setItemCaptionGenerator(DeploymentInstance::getFullPath);
        targetInstances.addStyleName(DatacTheme.FIELD_WIDE);
        progressWindow.setCaption("Quick deployment progress");
    }

    private Component buildContent() {
        MVerticalLayout content = new MVerticalLayout().withFullSize();
        MVerticalLayout form = new MVerticalLayout();
        form.with(headerLabel, noInstancesLabel, infoLabel, targetInstances);

        MHorizontalLayout buttonLayout = new MHorizontalLayout().with(deployButton, cancelButton);
        MVerticalLayout footer = new MVerticalLayout().withStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR)
                .withFullWidth().with(buttonLayout).withAlign(buttonLayout, Alignment.BOTTOM_RIGHT);


        content.with(form, footer).withExpandRatio(form, 1.0f);
        return content;
    }

    private void performDeployment(Button.ClickEvent event) {
        progressWindow.reset();
        UI.getCurrent().addWindow(progressWindow);
        deploymentManagerService.startAsynchronousQuickDeployment(project, targetInstances.getSelectedItems(), changeSet, progressWindowChangeHandler, this::handleDeploymentComplete);
    }

    private void handleDeploymentComplete(QuickDeploymentResult quickDeploymentResult) {
        getUI().access(() -> {
            progressWindow.setCloseButtonEnabled(true);

            if (quickDeploymentResult.isSuccessful()) {
                // TODO: Do some logging?
                progressWindow.setMessage("Quick deployment finished successfully");
            } else {
                progressWindow.setMessage("Quick deployment failed:\n" + quickDeploymentResult.getErrorMessage() + "\nCheck the event log for more details.");
            }
        });
    }

    void prepareWindow(DatabaseChangeSet changeSet, Revision revision) {
        this.changeSet = changeSet;
        this.revision = revision;
        this.project = revision.getProject();
        refreshInstances();
    }

    private void refreshInstances() {
        Set<DeploymentInstance> instances;
        /*if (compatibleInstancesOnly) {
            instances = instanceService.findInstancesWithTrackingRevision(revision);
        } else {*/
            instances = instanceService.findAllInProject(project);
        /*}*/
        boolean instancesAvailable = !instances.isEmpty();
        targetInstances.setEnabled(instancesAvailable);
        noInstancesLabel.setVisible(!instancesAvailable);
        deployButton.setEnabled(instancesAvailable);
        targetInstances.setItems(instances);
    }
}
