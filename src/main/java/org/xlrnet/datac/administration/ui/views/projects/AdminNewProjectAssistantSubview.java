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
import org.xlrnet.datac.commons.tasks.RunnableTask;
import org.xlrnet.datac.commons.ui.DatacTheme;
import org.xlrnet.datac.foundation.ui.views.AbstractSubview;
import org.xlrnet.datac.vcs.api.VcsAdapter;
import org.xlrnet.datac.vcs.api.VcsConnectionStatus;
import org.xlrnet.datac.vcs.api.VcsMetaInfo;
import org.xlrnet.datac.vcs.api.VcsRemoteCredentials;
import org.xlrnet.datac.vcs.services.VersionControlSystemService;
import org.xlrnet.datac.vcs.tasks.CheckRemoteVcsConnectionTask;

import java.util.Optional;

/**
 * Assistant for creating new projects.
 */
@SpringComponent
@SpringView(name = AdminNewProjectAssistantSubview.VIEW_NAME)
public class AdminNewProjectAssistantSubview extends AbstractSubview {

    static final String VIEW_NAME = "admin/projects/new";

    private static final int NOTIFICATION_DELAY_MS = 5000;

    /** The VCS Service. */
    private final VersionControlSystemService vcsService;

    /** Task executor. */
    private final TaskExecutor taskExecutor;

    /** Layout for project name field. */
    private TextField nameField = new TextField("Name");

    /** Layout for project description. */
    private TextArea descriptionArea = new TextArea("Description");

    /** Selection box for various VCS implementations. */
    private ComboBox<VcsMetaInfo> vcsSelect = new ComboBox<>("VCS System");

    /** Text field for vcs target URL. */
    @PropertyId("url")
    private TextField vcsUrlField = new TextField("URL");

    /** Text field for vcs username. */
    @PropertyId("username")
    private TextField vcsUsernameField = new TextField("Username");

    /** Text field for vcs password. */
    @PropertyId("password")
    private PasswordField vcsPasswordField = new PasswordField("Password");

    /** Progress bar for VCS setup. */
    private ProgressBar progressBar = new ProgressBar();

    /** Layout with main content. */
    private VerticalLayout mainLayout;

    /** Layout for buttons. */
    private HorizontalLayout buttonLayout;

    /** Validation binder. */
    private BeanValidationBinder<VcsRemoteCredentials> binder = new BeanValidationBinder<>(VcsRemoteCredentials.class);

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
        mainLayout.setSpacing(false);

        Layout informationLayout = buildInformationLayout();
        mainLayout.addComponent(informationLayout);

        Label section = new Label("VCS Setup");
        section.addStyleName(ValoTheme.LABEL_COLORED);
        mainLayout.addComponent(section);

        Layout vcsSetupLayout = buildVcsSetupLayout();
        mainLayout.addComponent(vcsSetupLayout);

        binder.bindInstanceFields(this);
        binder.setBean(new VcsRemoteCredentials());

        return mainLayout;
    }

    @NotNull
    private VerticalLayout buildInformationLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);

        layout.addComponent(nameField);
        layout.addComponent(descriptionArea);

        return layout;
    }

    @NotNull
    private VerticalLayout buildVcsSetupLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);

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

        Button continueButton = new Button("Continue");
        continueButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
        continueButton.addClickListener(event -> Notification.show("Not implemented."));
        Button checkConnectionButton = new Button("Test connection");
        checkConnectionButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
        checkConnectionButton.addClickListener(e -> {
            binder.validate();
            if (binder.isValid()) {
                this.checkConnection();
            }
        });
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> UI.getCurrent().getNavigator().navigateTo(AdminProjectSubview.VIEW_NAME));

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

    private void checkConnection() {
        Optional<VcsAdapter> adapter = vcsService.findAdapterByMetaInfo(vcsSelect.getValue());

        UI ui = UI.getCurrent();
        RunnableTask<VcsConnectionStatus> checkConnection = new CheckRemoteVcsConnectionTask(adapter.get(), binder.getBean());
        checkConnection.setRunningStatusHandler((running) -> ui.access(() -> setCheckingMode(running)));
        checkConnection.setEntityChangeHandler(e -> ui.access(() -> showConnectionNotification(e)));

        taskExecutor.execute(checkConnection);
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
        setVcsFieldEnabled(!checking);
        vcsSelect.setEnabled(!checking);
    }

    private void setVcsFieldEnabled(boolean enabled) {
        vcsUrlField.setEnabled(enabled);
        vcsUsernameField.setEnabled(enabled);
        vcsPasswordField.setEnabled(enabled);
        buttonLayout.setEnabled(enabled);
    }
}
