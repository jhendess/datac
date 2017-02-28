package org.xlrnet.datac.administration.ui.views.projects;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.commons.ui.DatacTheme;
import org.xlrnet.datac.foundation.ui.views.AbstractSubview;
import org.xlrnet.datac.vcs.api.VcsMetaInfo;
import org.xlrnet.datac.vcs.services.VersionControlSystemService;

/**
 * Assistant for creating new projects.
 */
@SpringComponent
@SpringView(name = AdminNewProjectAssistantSubview.VIEW_NAME)
public class AdminNewProjectAssistantSubview extends AbstractSubview {

    static final String VIEW_NAME = "admin/projects/new";

    /** The VCS Service. */
    private final VersionControlSystemService vcsService;

    /** Layout for project name field. */
    private TextField nameField = new TextField("Name");

    /** Layout for project description. */
    private TextArea descriptionArea = new TextArea("Description");

    /** Selection box for various VCS implementations. */
    private NativeSelect<VcsMetaInfo> vcsSelect = new NativeSelect<>("VCS System");

    private TextField vcsUrlField = new TextField("URL");

    private TextField vcsUsernameField = new TextField("Username");

    private PasswordField vcsPasswordField = new PasswordField("Password");

    /** Layout with main content. */
    private VerticalLayout mainLayout;

    /** Layout for buttons. */
    private HorizontalLayout buttonLayout;

    @Autowired
    public AdminNewProjectAssistantSubview(VersionControlSystemService vcsService) {
        this.vcsService = vcsService;
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

        return mainLayout;
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
        checkConnectionButton.addClickListener(event -> Notification.show("Not implemented."));
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> UI.getCurrent().getNavigator().navigateTo(AdminProjectSubview.VIEW_NAME));

        buttonLayout.addComponent(continueButton);
        buttonLayout.addComponent(checkConnectionButton);
        buttonLayout.addComponent(cancelButton);

        layout.addComponent(buttonLayout);

        setVcsFieldEnabled(false);
        return layout;
    }

    private void setVcsFieldEnabled(boolean enabled) {
        vcsUrlField.setEnabled(enabled);
        vcsUsernameField.setEnabled(enabled);
        vcsPasswordField.setEnabled(enabled);
        buttonLayout.setEnabled(enabled);
    }

    @NotNull
    private VerticalLayout buildInformationLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);

        layout.addComponent(nameField);
        layout.addComponent(descriptionArea);

        return layout;
    }
}
