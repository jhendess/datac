package org.xlrnet.datac.administration.ui.views.projects;

import com.vaadin.annotations.PropertyId;
import com.vaadin.data.BeanValidationBinder;
import com.vaadin.server.Page;
import com.vaadin.shared.Position;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.vaadin.viritin.fields.IntegerField;
import org.xlrnet.datac.commons.tasks.RunnableTask;
import org.xlrnet.datac.commons.ui.DatacTheme;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.ui.components.BooleanStatusChangeHandler;
import org.xlrnet.datac.foundation.ui.components.EntityChangeHandler;
import org.xlrnet.datac.foundation.ui.views.AbstractSubview;
import org.xlrnet.datac.vcs.api.VcsAdapter;
import org.xlrnet.datac.vcs.api.VcsConnectionStatus;
import org.xlrnet.datac.vcs.api.VcsMetaInfo;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.VcsConfig;
import org.xlrnet.datac.vcs.services.VersionControlSystemService;
import org.xlrnet.datac.vcs.tasks.CheckRemoteVcsConnectionTask;
import org.xlrnet.datac.vcs.tasks.FetchRemoteVcsBranchesTask;

import java.util.Collection;
import java.util.Optional;

/**
 * Assistant for creating new projects.
 */
@SpringComponent
@SpringView(name = AdminNewProjectAssistantSubview.VIEW_NAME)
public class AdminNewProjectAssistantSubview extends AbstractSubview {

    static final String VIEW_NAME = "admin/projects/new";

    private static final int NOTIFICATION_DELAY_MS = 5000;

    /**
     * The VCS Service.
     */
    private final VersionControlSystemService vcsService;

    /**
     * Task executor.
     */
    private final TaskExecutor taskExecutor;

    /**
     * Layout for project name field.
     */
    @PropertyId("name")
    private final TextField nameField = new TextField("Name");

    /**
     * Layout for project description.
     */
    @PropertyId("description")
    private final TextArea descriptionArea = new TextArea("Description");

    /**
     * Selection box for various VCS implementations.
     */
    private final ComboBox<VcsMetaInfo> vcsSelect = new ComboBox<>("VCS System");

    /**
     * Text field for vcs target URL.
     */
    @PropertyId("url")
    private final TextField vcsUrlField = new TextField("URL");

    /**
     * Text field for vcs username.
     */
    @PropertyId("username")
    private final TextField vcsUsernameField = new TextField("Username");

    /**
     * Text field for vcs password.
     */
    @PropertyId("password")
    private final PasswordField vcsPasswordField = new PasswordField("Password");

    /**
     * Selection box for the VCS development branch.
     */
    private final ComboBox<Branch> branchSelect = new ComboBox<>("Development branch");

    /**
     * Poll interval for new VCS.
     */
    @PropertyId("pollInterval")
    private final IntegerField pollIntervalField = new IntegerField("Poll interval in minutes");

    /**
     * Checkboxes for selecting release branches.
     */
    private final CheckBoxGroup<Branch> releaseBranchesCheckboxGroup = new CheckBoxGroup<>("Release branches");

    /**
     * Checkbox to enable automatic import of new branches.
     */
    @PropertyId("newBranchPattern")
    private final TextField newBranchesPattern = new TextField("Pattern for new branches");

    /**
     * Text field for changelog master file.
     */
    @PropertyId("changelogLocation")
    private final TextField changeLogLocationField = new TextField("Changelog master file");

    /**
     * Progress bar for VCS setup.
     */
    private final ProgressBar progressBar = new ProgressBar();

    /**
     * Layout with main content.
     */
    private VerticalLayout mainLayout;

    /**
     * Layout for buttons.
     */
    private HorizontalLayout buttonLayout;

    /**
     * Project validation projectBinder.
     */
    private final BeanValidationBinder<Project> projectBinder = new BeanValidationBinder<>(Project.class);

    /**
     * VCS validation projectBinder.
     */
    private final BeanValidationBinder<VcsConfig> vcsBinder = new BeanValidationBinder<>(VcsConfig.class);

    @Autowired
    public AdminNewProjectAssistantSubview(VersionControlSystemService vcsService, TaskExecutor taskExecutor) {
        this.vcsService = vcsService;
        this.taskExecutor = taskExecutor;
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        return "This assistant will help you setting up a new project and configure branches and versions to track.";
    }

    @NotNull
    @Override
    protected String getTitle() {
        return "New project";
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        nameField.addStyleName(DatacTheme.FIELD_WIDE);
        descriptionArea.addStyleName(DatacTheme.FIELD_WIDE);
        vcsSelect.addStyleName(DatacTheme.FIELD_WIDE);
        vcsUrlField.addStyleName(DatacTheme.FIELD_WIDE);
        vcsUsernameField.addStyleName(DatacTheme.FIELD_WIDE);
        vcsPasswordField.addStyleName(DatacTheme.FIELD_WIDE);

        mainLayout = new VerticalLayout();
        mainLayout.setMargin(false);

        Layout informationLayout = buildInformationLayout();
        mainLayout.addComponent(informationLayout);

        Label section = new Label("VCS Setup");
        section.addStyleName(ValoTheme.LABEL_COLORED);
        mainLayout.addComponent(section);

        Layout vcsSetupLayout = buildVcsSetupLayout();
        mainLayout.addComponent(vcsSetupLayout);

        projectBinder.bindInstanceFields(this);
        projectBinder.setBean(new Project());
        vcsBinder.bindInstanceFields(this);
        VcsConfig vcsConfig = new VcsConfig();
        vcsConfig.setChangelogLocation("CHANGEME");
        vcsConfig.setNewBranchPattern(".+");
        vcsBinder.setBean(vcsConfig);

        return mainLayout;
    }

    @NotNull
    private Layout buildInformationLayout() {
        FormLayout layout = new FormLayout();
        layout.setSpacing(true);

        layout.addComponent(nameField);
        layout.addComponent(descriptionArea);

        return layout;
    }

    @NotNull
    private Layout buildVcsSetupLayout() {
        FormLayout layout = new FormLayout();

        vcsSelect.setDescription("Version control system that is used");
        vcsUrlField.setDescription("URL which is used for fetching data from a repository");
        vcsUsernameField.setDescription("Leave blank if anonymous access should be used.");

        vcsSelect.setItems(vcsService.listSupportedVersionControlSystems());
        vcsSelect.setItemCaptionGenerator(m -> String.format("%s (%s)", m.getVcsName(), m.getAdapterName()));
        vcsSelect.addValueChangeListener(c -> {
            setVcsFieldEnabled(c.getValue() != null);
        });

        layout.addComponent(vcsSelect);
        layout.addComponent(vcsUrlField);
        layout.addComponent(vcsUsernameField);
        layout.addComponent(vcsPasswordField);

        buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(new MarginInfo(true, false));
        buttonLayout.setSpacing(true);

        UI ui = UI.getCurrent();

        Button continueButton = new Button("Continue");
        continueButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
        continueButton.addClickListener(e -> {
            projectBinder.validate();
            vcsBinder.validate();
            if (vcsBinder.isValid() && projectBinder.isValid()) {
                checkConnection(buildContinueButtonHandler(ui));
                changeLogLocationField.setValue("");
            }
        });
        Button checkConnectionButton = new Button("Test connection");
        checkConnectionButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
        checkConnectionButton.addClickListener(e -> {
            vcsBinder.validate();
            if (vcsBinder.isValid()) {
                this.checkConnection((s) -> ui.access(() -> showConnectionNotification(s)));
            }
        });
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> ui.getNavigator().navigateTo(AdminProjectSubview.VIEW_NAME));

        buttonLayout.addComponent(continueButton);
        buttonLayout.addComponent(checkConnectionButton);
        buttonLayout.addComponent(cancelButton);

        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        buttonLayout.addComponent(progressBar);

        layout.addComponent(buttonLayout);

        setVcsFieldEnabled(false);
        return layout;
    }

    @NotNull
    private EntityChangeHandler<VcsConnectionStatus> buildContinueButtonHandler(UI ui) {
        return (s) -> {
            // Only show a notification if something went wrong
            if (!(VcsConnectionStatus.ESTABLISHED.equals(s))) {
                ui.access(() -> showConnectionNotification(s));
                return;
            }
            Optional<VcsAdapter> adapter = vcsService.findAdapterByMetaInfo(vcsSelect.getValue());

            FetchRemoteVcsBranchesTask fetchBranches = new FetchRemoteVcsBranchesTask(adapter.get(), vcsBinder.getBean());
            fetchBranches.setRunningStatusHandler(buildRunningStatusHandler(ui));
            fetchBranches.setEntityChangeHandler(branches -> ui.access(() -> handleFetchedBranches(branches)));
            taskExecutor.execute(fetchBranches);
        };
    }

    private void checkConnection(@NotNull EntityChangeHandler<VcsConnectionStatus> entityChangeHandler) {
        Optional<VcsAdapter> adapter = vcsService.findAdapterByMetaInfo(vcsSelect.getValue());

        UI ui = UI.getCurrent();
        RunnableTask<VcsConnectionStatus> checkConnection = new CheckRemoteVcsConnectionTask(adapter.get(), vcsBinder.getBean());
        checkConnection.setRunningStatusHandler(buildRunningStatusHandler(ui));
        checkConnection.setEntityChangeHandler(entityChangeHandler);

        taskExecutor.execute(checkConnection);
    }

    @NotNull
    private BooleanStatusChangeHandler buildRunningStatusHandler(UI ui) {
        return (running) -> ui.access(() -> setCheckingMode(running));
    }

    private void handleFetchedBranches(Collection<Branch> branches) {
        if (branches == null) {
            Notification notification = new Notification("Fetching branches failed", Notification.Type.ERROR_MESSAGE);
            notification.setPosition(Position.BOTTOM_RIGHT);
            notification.show(Page.getCurrent());
        } else {
            changeToBranchSelectionState(branches);
        }
    }

    private void changeToBranchSelectionState(Collection<Branch> branches) {
        FormLayout branchSelectionLayout = new FormLayout();

        vcsBinder.getBean().setBranches(branches);

        branchSelect.setItems(branches);
        branchSelect.setItemCaptionGenerator(Branch::getName);
        releaseBranchesCheckboxGroup.setItems(branches);
        releaseBranchesCheckboxGroup.setItemCaptionGenerator(Branch::getName);
        releaseBranchesCheckboxGroup.addValueChangeListener(selected -> {
            // Update the selection state in the source objects
            for (Branch branch : branches) {
                branch.setWatched(selected.getValue().contains(branch));
            }
        });

        Button continueButton = new Button("Continue");
        continueButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
        continueButton.addClickListener(e -> {
            vcsBinder.validate();
            if (vcsBinder.isValid()) {
                Notification.show("Not implemented");
            }
        });
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> UI.getCurrent().getNavigator().navigateTo(AdminProjectSubview.VIEW_NAME));

        buttonLayout.removeAllComponents();
        buttonLayout.addComponent(continueButton);
        buttonLayout.addComponent(cancelButton);

        branchSelectionLayout.addComponent(changeLogLocationField);
        branchSelectionLayout.addComponent(pollIntervalField);
        branchSelectionLayout.addComponent(branchSelect);
        branchSelectionLayout.addComponent(releaseBranchesCheckboxGroup);
        branchSelectionLayout.addComponent(newBranchesPattern);
        branchSelectionLayout.addComponent(buttonLayout);

        mainLayout.removeAllComponents();
        mainLayout.addComponent(branchSelectionLayout);
    }

    private void showConnectionNotification(@NotNull VcsConnectionStatus e) {
        Notification notification = new Notification(e.name(), Notification.Type.ERROR_MESSAGE);
        notification.setPosition(Position.BOTTOM_RIGHT);
        notification.setDelayMsec(NOTIFICATION_DELAY_MS);
        if (VcsConnectionStatus.ESTABLISHED.equals(e)) {
            notification.setStyleName(ValoTheme.NOTIFICATION_SUCCESS);
        }
        notification.show(Page.getCurrent());
    }

    private void setCheckingMode(boolean checking) {
        progressBar.setVisible(checking);
        mainLayout.setEnabled(!checking);
    }

    private void setVcsFieldEnabled(boolean enabled) {
        vcsUrlField.setEnabled(enabled);
        vcsUsernameField.setEnabled(enabled);
        vcsPasswordField.setEnabled(enabled);
        buttonLayout.setEnabled(enabled);
    }
}
