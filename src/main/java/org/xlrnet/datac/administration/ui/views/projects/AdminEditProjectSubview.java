package org.xlrnet.datac.administration.ui.views.projects;

import com.google.common.base.Objects;
import com.vaadin.annotations.PropertyId;
import com.vaadin.data.BeanValidationBinder;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.vaadin.viritin.fields.IntegerField;
import org.xlrnet.datac.commons.tasks.RunnableTask;
import org.xlrnet.datac.commons.ui.DatacTheme;
import org.xlrnet.datac.commons.ui.NotificationUtils;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.domain.ProjectState;
import org.xlrnet.datac.foundation.services.ProjectService;
import org.xlrnet.datac.foundation.ui.components.BooleanStatusChangeHandler;
import org.xlrnet.datac.foundation.ui.components.EntityChangeHandler;
import org.xlrnet.datac.foundation.ui.components.SimpleOkCancelWindow;
import org.xlrnet.datac.foundation.ui.views.AbstractSubview;
import org.xlrnet.datac.vcs.api.VcsAdapter;
import org.xlrnet.datac.vcs.api.VcsConnectionStatus;
import org.xlrnet.datac.vcs.api.VcsMetaInfo;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.services.LockingService;
import org.xlrnet.datac.vcs.services.VersionControlSystemService;
import org.xlrnet.datac.vcs.tasks.CheckRemoteVcsConnectionTask;
import org.xlrnet.datac.vcs.tasks.FetchRemoteVcsBranchesTask;

import javax.validation.ConstraintViolationException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Assistant for creating new projects.
 */
@SpringComponent
@SpringView(name = AdminEditProjectSubview.VIEW_NAME)
public class AdminEditProjectSubview extends AbstractSubview {

    static final String VIEW_NAME = "admin/projects/edit";

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminEditProjectSubview.class);

    /**
     * The VCS Service.
     */
    private final VersionControlSystemService vcsService;

    /**
     * Task executor.
     */
    private final TaskExecutor taskExecutor;

    /**
     * The project service.
     */
    private final ProjectService projectService;

    /**
     * The central locking service.
     */
    private final LockingService lockingService;

    /**
     * Text field for project name.
     */
    @PropertyId("name")
    private final TextField nameField = new TextField("Name");

    /**
     * Text area for project description.
     */
    @PropertyId("description")
    private final TextArea descriptionArea = new TextArea("Description");

    /**
     * Text field for the project website.
     */
    @PropertyId("website")
    private final TextField websiteField = new TextField("Website");

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
    private final ComboBox<Branch> vcsDevBranchSelect = new ComboBox<>("Development branch");

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
     * Bound backing bean for the project.
     */
    private Project projectBean;

    /**
     * Flag to indicate if this is a new project.
     */
    private boolean isNewProject;

    /**
     * The old URL for the remote VCS.
     */
    private String oldVcsUrl;

    /**
     * Layout for VCS settings.
     */
    private FormLayout vcsSettingsLayout;

    private Button cancelButton;

    private Button checkConnectionButton;

    private Button continueButton;

    @Autowired
    public AdminEditProjectSubview(VersionControlSystemService vcsService, @Qualifier("defaultTaskExecutor") TaskExecutor taskExecutor, ProjectService projectService, LockingService lockingService) {
        this.vcsService = vcsService;
        this.taskExecutor = taskExecutor;
        this.projectService = projectService;
        this.lockingService = lockingService;
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

    /**
     * If the current navigator state ends with a valid integer, return the project with the matching id. If the state
     * has is no integer, null will returned (indicating that a new project should be created).
     */
    private Project getProjectToEdit() {
        if (NumberUtils.isDigits(getParameters())) {
            LOGGER.debug("Trying to open project {} for editing", getParameters());
            long id = Long.parseLong(getParameters());
            return projectService.findOne(id);
        } else {
            return null;
        }
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        nameField.addStyleName(DatacTheme.FIELD_WIDE);
        descriptionArea.addStyleName(DatacTheme.FIELD_WIDE);
        websiteField.addStyleName(DatacTheme.FIELD_WIDE);
        vcsSelect.addStyleName(DatacTheme.FIELD_WIDE);
        vcsUrlField.addStyleName(DatacTheme.FIELD_WIDE);
        vcsUsernameField.addStyleName(DatacTheme.FIELD_WIDE);
        vcsPasswordField.addStyleName(DatacTheme.FIELD_WIDE);

        changeLogLocationField.addStyleName(DatacTheme.FIELD_WIDE);
        pollIntervalField.addStyleName(DatacTheme.FIELD_WIDE);
        vcsDevBranchSelect.addStyleName(DatacTheme.FIELD_WIDE);
        newBranchesPattern.addStyleName(DatacTheme.FIELD_WIDE);

        mainLayout = new VerticalLayout();
        mainLayout.setMargin(false);

        Layout informationLayout = buildInformationLayout();
        mainLayout.addComponent(informationLayout);

        Label section = new Label("VCS Setup");
        section.addStyleName(ValoTheme.LABEL_COLORED);
        mainLayout.addComponent(section);

        Layout vcsSetupLayout = buildVcsSetupLayout();
        mainLayout.addComponent(vcsSetupLayout);

        setupProject();
        if (!isNewProject) {
            changeToBranchSelectionState(projectBean.getBranches());
            vcsDevBranchSelect.setValue(projectBean.getDevelopmentBranch());
            Optional<VcsMetaInfo> metaInfo = vcsService.findMetaInfoByAdapterClassName(projectBean.getAdapterClass());
            if (!metaInfo.isPresent()) {
                metaInfo = vcsService.findMetaInfoByVcsType(projectBean.getType());
            }
            if (metaInfo.isPresent()) {
                vcsSelect.setValue(metaInfo.get());
                setVcsFieldEnabled(true);
            } else {
                NotificationUtils.showError("The VCS adapter used for creating the project is missing.", true);
            }
        }

        return mainLayout;
    }

    private void setupProject() {
        projectBean = getProjectToEdit();
        if (projectBean == null) {
            projectBean = new Project();
            isNewProject = true;
            projectBean.setChangelogLocation("CHANGEME");
            projectBean.setNewBranchPattern(".+");
            projectBean.setState(ProjectState.NEW);
        }
        oldVcsUrl = projectBean.getUrl();
        projectBinder.bindInstanceFields(this);
        projectBinder.setBean(projectBean);
    }

    @NotNull
    private Layout buildInformationLayout() {
        FormLayout layout = new FormLayout();
        layout.setSpacing(true);

        layout.addComponent(nameField);
        layout.addComponent(websiteField);
        layout.addComponent(descriptionArea);

        return layout;
    }

    @NotNull
    private Layout buildVcsSetupLayout() {
        vcsSettingsLayout = new FormLayout();

        vcsSelect.setDescription("Version control system that is used");
        vcsUrlField.setDescription("URL which is used for fetching data from a repository");
        vcsUsernameField.setDescription("Leave blank if anonymous access should be used.");

        vcsSelect.setItems(vcsService.listSupportedVersionControlSystems());
        vcsSelect.setItemCaptionGenerator(m -> String.format("%s (%s)", m.getVcsName(), m.getAdapterName()));
        vcsSelect.addValueChangeListener(c -> setVcsFieldEnabled(c.getValue() != null));

        vcsSettingsLayout.addComponent(vcsSelect);
        vcsSettingsLayout.addComponent(vcsUrlField);
        vcsSettingsLayout.addComponent(vcsUsernameField);
        vcsSettingsLayout.addComponent(vcsPasswordField);

        buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(new MarginInfo(true, false));
        buttonLayout.setSpacing(true);

        UI ui = UI.getCurrent();

        continueButton = new Button("Continue");
        continueButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
        continueButton.addClickListener(e -> {
            projectBinder.validate();
            if (projectBinder.isValid()) {
                checkConnection(buildContinueButtonHandler(ui));
                changeLogLocationField.setValue("");
            }
        });
        checkConnectionButton = new Button("Test connection");
        checkConnectionButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
        checkConnectionButton.addClickListener(e -> {
            projectBinder.validate();
            if (projectBinder.isValid()) {
                this.checkConnection((s) -> ui.access(() -> showConnectionNotification(s)));
            }
        });
        cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> ui.getNavigator().navigateTo(AdminProjectSubview.VIEW_NAME));

        buttonLayout.addComponent(continueButton);
        buttonLayout.addComponent(checkConnectionButton);
        buttonLayout.addComponent(cancelButton);

        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        buttonLayout.addComponent(progressBar);

        vcsSettingsLayout.addComponent(buttonLayout);

        setVcsFieldEnabled(false);
        return vcsSettingsLayout;
    }

    @NotNull
    private EntityChangeHandler<VcsConnectionStatus> buildContinueButtonHandler(UI ui) {
        return (s) -> {
            // Show a notification if something went wrong
            if (!(VcsConnectionStatus.ESTABLISHED.equals(s))) {
                if (isNewProject) {
                    changeLogLocationField.setValue("CHANGEME");
                }
                ui.access(() -> showConnectionNotification(s));
                return;
            }
            Optional<VcsAdapter> adapter = vcsService.findAdapterByMetaInfo(vcsSelect.getValue());

            FetchRemoteVcsBranchesTask fetchBranches = new FetchRemoteVcsBranchesTask(adapter.get(), projectBean);
            fetchBranches.setRunningStatusHandler(buildRunningStatusHandler(ui));
            fetchBranches.setEntityChangeHandler(branches -> ui.access(() -> handleFetchedBranches(branches)));
            taskExecutor.execute(fetchBranches);
        };
    }

    private void checkConnection(@NotNull EntityChangeHandler<VcsConnectionStatus> entityChangeHandler) {
        Optional<VcsAdapter> adapter = vcsService.findAdapterByMetaInfo(vcsSelect.getValue());

        UI ui = UI.getCurrent();
        RunnableTask<VcsConnectionStatus> checkConnection = new CheckRemoteVcsConnectionTask(adapter.get(), projectBean);
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
            NotificationUtils.showError("Fetching branches failed", null, true);
        } else {
            // vcsSettingsLayout.removeAllComponents();
            changeToBranchSelectionState(branches);
        }
    }

    private void changeToBranchSelectionState(Collection<Branch> branches) {
        projectBean.setBranches(branches);

        vcsDevBranchSelect.setItems(branches);
        vcsDevBranchSelect.setItemCaptionGenerator(Branch::getName);
        vcsDevBranchSelect.addValueChangeListener(vc -> {
            if (vc.getOldValue() != null) {
                vc.getOldValue().setDevelopment(false);
            }
            vc.getValue().setDevelopment(true);
        });
        releaseBranchesCheckboxGroup.setItems(branches);
        releaseBranchesCheckboxGroup.setItemCaptionGenerator(Branch::getName);
        releaseBranchesCheckboxGroup.setValue(
                branches.stream().filter(Branch::isWatched).collect(Collectors.toSet())
        );
        releaseBranchesCheckboxGroup.addValueChangeListener(selected -> {
            // Update the selection state in the source objects
            for (Branch branch : branches) {
                branch.setWatched(selected.getValue().contains(branch));
            }
        });
        if (isNewProject) {
            vcsDevBranchSelect.setValue(projectBean.getDevelopmentBranch());
        }

        Button continueButton = new Button("Save");
        continueButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
        continueButton.addClickListener(e -> {
            projectBinder.validate();
            if (projectBinder.isValid()) {
                prepareBeansForSaving();
            }
        });
        cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> UI.getCurrent().getNavigator().navigateTo(AdminProjectSubview.VIEW_NAME));

        /*Button branchReloadButton = new Button("Reload branches");
        branchReloadButton.addClickListener(e -> {
            checkConnection(buildContinueButtonHandler(VaadinUI.getCurrent()));
        });*/

        buttonLayout.removeAllComponents();
        buttonLayout.addComponent(continueButton);
        buttonLayout.addComponent(cancelButton);
        //buttonLayout.addComponent(branchReloadButton);
        buttonLayout.addComponent(progressBar);

        vcsSettingsLayout.addComponent(changeLogLocationField);
        vcsSettingsLayout.addComponent(pollIntervalField);
        vcsSettingsLayout.addComponent(vcsDevBranchSelect);
        vcsSettingsLayout.addComponent(releaseBranchesCheckboxGroup);
        vcsSettingsLayout.addComponent(newBranchesPattern);
        vcsSettingsLayout.addComponent(buttonLayout);
    }

    private void saveProject() {
        boolean locked = false;
        if (!isNewProject) {
            locked = lockingService.tryLock(projectBean);
        }
        if (isNewProject || locked) {
            try {
                projectService.saveProject(projectBean);
                NotificationUtils.showSuccess("Project saved successfully!");
                UI.getCurrent().getNavigator().navigateTo(AdminProjectSubview.VIEW_NAME);
            } catch (ConstraintViolationException cve) {    // NOSONAR: Logging of exception not necessary
                LOGGER.warn("Saving project failed due to constraint violations");
                NotificationUtils.showValidationError("Saving failed", cve.getConstraintViolations());
            } catch (RuntimeException e) {
                LOGGER.error("Saving project failed", e);
                NotificationUtils.showError("Saving failed", e.getMessage(), true);
            } finally {
                if (locked) {
                    lockingService.unlock(projectBean);
                }
            }
        } else {
            NotificationUtils.showError("Project locked.", false);
        }
    }

    private void prepareBeansForSaving() {
        VcsAdapter vcsAdapter = vcsService.findAdapterByMetaInfo(vcsSelect.getValue()).get();
        projectBean.setAdapterClass(vcsAdapter.getClass().getName());
        projectBean.setType(vcsAdapter.getMetaInfo().getVcsName());

        if (!isNewProject && !StringUtils.equalsIgnoreCase(oldVcsUrl, projectBean.getUrl()) && projectBean.isInitialized()) {
            SimpleOkCancelWindow vcsChangeCheckWindow = new SimpleOkCancelWindow("VCS changed", "Yes", "No");
            vcsChangeCheckWindow.setCustomContent(new Label("VCS URL has changed.<br>Do you want to reinitialize the VCS on the next update?", ContentMode.HTML));
            vcsChangeCheckWindow.setOkHandler(() -> {
                projectBean.setInitialized(false);
                saveProject();
                vcsChangeCheckWindow.close();
            });
            vcsChangeCheckWindow.setCancelHandler(() -> {
                saveProject();
                vcsChangeCheckWindow.close();
            });
            UI.getCurrent().addWindow(vcsChangeCheckWindow);
        } else {
            saveProject();
        }
    }

    private void showConnectionNotification(@NotNull VcsConnectionStatus e) {
        if (VcsConnectionStatus.ESTABLISHED.equals(e)) {
            NotificationUtils.showSuccess("Connection established");
        } else {
            NotificationUtils.showWarning(e.name());
        }
    }

    private void setCheckingMode(boolean checking) {
        progressBar.setVisible(checking);
        mainLayout.setEnabled(!checking);
    }

    private void setVcsFieldEnabled(boolean enabled) {
        vcsUrlField.setEnabled(enabled);
        vcsUsernameField.setEnabled(enabled);
        vcsPasswordField.setEnabled(enabled);
        checkConnectionButton.setEnabled(enabled);
        continueButton.setEnabled(enabled);
        cancelButton.setEnabled(true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdminEditProjectSubview)) return false;
        if (!super.equals(o)) return false;
        AdminEditProjectSubview that = (AdminEditProjectSubview) o;
        return Objects.equal(vcsService, that.vcsService) &&
                Objects.equal(taskExecutor, that.taskExecutor) &&
                Objects.equal(projectService, that.projectService) &&
                Objects.equal(nameField, that.nameField) &&
                Objects.equal(descriptionArea, that.descriptionArea) &&
                Objects.equal(websiteField, that.websiteField) &&
                Objects.equal(vcsSelect, that.vcsSelect) &&
                Objects.equal(vcsUrlField, that.vcsUrlField) &&
                Objects.equal(vcsUsernameField, that.vcsUsernameField) &&
                Objects.equal(vcsPasswordField, that.vcsPasswordField) &&
                Objects.equal(vcsDevBranchSelect, that.vcsDevBranchSelect) &&
                Objects.equal(pollIntervalField, that.pollIntervalField) &&
                Objects.equal(releaseBranchesCheckboxGroup, that.releaseBranchesCheckboxGroup) &&
                Objects.equal(newBranchesPattern, that.newBranchesPattern) &&
                Objects.equal(changeLogLocationField, that.changeLogLocationField) &&
                Objects.equal(progressBar, that.progressBar) &&
                Objects.equal(mainLayout, that.mainLayout) &&
                Objects.equal(buttonLayout, that.buttonLayout) &&
                Objects.equal(projectBinder, that.projectBinder) &&
                Objects.equal(projectBean, that.projectBean);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), vcsService, taskExecutor, projectService, nameField, descriptionArea, websiteField, vcsSelect, vcsUrlField, vcsUsernameField, vcsPasswordField, vcsDevBranchSelect, pollIntervalField, releaseBranchesCheckboxGroup, newBranchesPattern, changeLogLocationField, progressBar, mainLayout, buttonLayout, projectBinder, projectBean);
    }
}
