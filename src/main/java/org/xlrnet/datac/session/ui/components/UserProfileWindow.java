package org.xlrnet.datac.session.ui.components;

import com.vaadin.annotations.PropertyId;
import com.vaadin.data.BeanValidationBinder;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.commons.ui.NotificationUtils;
import org.xlrnet.datac.commons.util.WindowUtils;
import org.xlrnet.datac.session.domain.User;
import org.xlrnet.datac.session.services.CryptoService;
import org.xlrnet.datac.session.services.UserService;

import javax.annotation.PostConstruct;
import java.util.Objects;

@UIScope
@SpringComponent
@SuppressWarnings("serial")
public class UserProfileWindow extends Window {

    public static final String ID = "profilepreferenceswindow";

    /**
     * Form binder for user data.
     */
    private final BeanValidationBinder<User> userBinder = new BeanValidationBinder<>(User.class);

    /**
     * Form Binder for password data.
     */
    private final BeanValidationBinder<PasswordData> passwordBinder = new BeanValidationBinder<>(PasswordData.class);

    /**
     * User service.
     */
    private final UserService userService;

    /**
     * Password service.
     */
    private final CryptoService cryptoService;

    /**
     * Text field for login name.
     */
    @PropertyId("loginName")
    private TextField loginNameField;

    /**
     * Text field for first name.
     */
    @PropertyId("firstName")
    private TextField firstNameField;

    /**
     * Text field for last name.
     */
    @PropertyId("lastName")
    private TextField lastNameField;

    /**
     * Text field for email.
     */
    @PropertyId("email")
    private TextField emailField;

    /**
     * Password field for old password.
     */
    @PropertyId("oldPassword")
    private PasswordField oldPasswordField;

    /**
     * Password field for new password.
     */
    @PropertyId("newPassword")
    private PasswordField newPasswordField;

    /**
     * Password field for confirmation of new password.
     */
    @PropertyId("newPasswordConfirmation")
    private PasswordField newPasswordConfirmationField;

    /**
     * User bean with last session data.
     */
    private User sessionUser;

    /**
     * Password data bean which contains the new passwords.
     */
    private PasswordData passwordData;

    /**
     * Cancel button.
     */
    private Button cancelButton;

    @Autowired
    private UserProfileWindow(UserService userService, CryptoService cryptoService) {
        this.userService = userService;
        this.cryptoService = cryptoService;
    }

    @PostConstruct
    private void init() {
        addStyleName("profile-window");
        setId(ID);

        setModal(true);
        setResizable(false);
        setClosable(false);
        setWidth(600, Unit.PIXELS);
        setHeight(520, Unit.PIXELS);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setMargin(new MarginInfo(true, false, false, false));
        setContent(content);

        TabSheet detailsWrapper = new TabSheet();
        detailsWrapper.setSizeFull();
        detailsWrapper.addStyleName(ValoTheme.TABSHEET_PADDED_TABBAR);
        detailsWrapper.addStyleName(ValoTheme.TABSHEET_ICONS_ON_TOP);
        detailsWrapper.addStyleName(ValoTheme.TABSHEET_CENTERED_TABS);
        content.addComponent(detailsWrapper);
        content.setExpandRatio(detailsWrapper, 1f);

        detailsWrapper.addComponent(buildProfileTab());
        detailsWrapper.addComponent(buildPreferencesTab());

        content.addComponent(buildFooter());

        // Bind fields to different binders
        userBinder.bindInstanceFields(this);
        passwordBinder.bindInstanceFields(this);
    }

    public void open() {
        sessionUser = userService.getSessionUser();
        passwordData = new PasswordData();
        passwordBinder.setBean(passwordData);
        userBinder.setBean(sessionUser);
        cancelButton.setVisible(!sessionUser.isPwChangeNecessary());
        UI.getCurrent().addWindow(this);
        if (sessionUser.isPwChangeNecessary()) {
            WindowUtils.showModalDialog("", "Password change necessary.");
        }
    }

    private Component buildPreferencesTab() {
        VerticalLayout root = new VerticalLayout();
        root.setCaption("Preferences");
        root.setIcon(VaadinIcons.COGS);
        root.setSpacing(true);
        root.setMargin(true);
        root.setSizeFull();

        Label message = new Label("Not implemented.");
        message.setSizeUndefined();
        message.addStyleName(ValoTheme.LABEL_LIGHT);
        root.addComponent(message);
        root.setComponentAlignment(message, Alignment.MIDDLE_CENTER);

        return root;
    }

    private Component buildProfileTab() {
        HorizontalLayout root = new HorizontalLayout();
        root.setCaption("Profile");
        root.setIcon(VaadinIcons.USER);
        root.setWidth(100.0f, Unit.PERCENTAGE);
        root.setSpacing(true);
        root.setMargin(true);
        root.addStyleName("profile-form");

        VerticalLayout pic = new VerticalLayout();
        pic.setSizeUndefined();
        pic.setSpacing(true);
        Image profilePic = new Image(null, new ThemeResource(
                "img/profile-pic-300px.jpg"));
        profilePic.setWidth(100.0f, Unit.PIXELS);
        pic.addComponent(profilePic);

        Button upload = new Button("Change…", (ClickListener) event -> NotificationUtils.showNotImplemented());
        upload.addStyleName(ValoTheme.BUTTON_TINY);
        pic.addComponent(upload);

        root.addComponent(pic);

        FormLayout details = new FormLayout();
        details.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);
        root.addComponent(details);
        root.setExpandRatio(details, 1);

        loginNameField = new TextField("Login Name");
        loginNameField.setEnabled(false);
        details.addComponent(loginNameField);
        firstNameField = new TextField("First Name");
        details.addComponent(firstNameField);
        lastNameField = new TextField("Last Name");
        details.addComponent(lastNameField);
        emailField = new TextField("E-Mail address");
        details.addComponent(emailField);

        Label section = new Label("Change password");
        section.addStyleName(ValoTheme.LABEL_H4);
        section.addStyleName(ValoTheme.LABEL_COLORED);
        details.addComponent(section);

        oldPasswordField = new PasswordField("Old password");
        details.addComponent(oldPasswordField);
        newPasswordField = new PasswordField("New password");
        details.addComponent(newPasswordField);
        newPasswordConfirmationField = new PasswordField("Confirm new password");
        details.addComponent(newPasswordConfirmationField);

        return root;
    }

    private Component buildFooter() {
        HorizontalLayout footer = new HorizontalLayout();
        footer.addStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
        footer.setWidth(100.0f, Unit.PERCENTAGE);

        Button ok = new Button("OK");
        ok.addStyleName(ValoTheme.BUTTON_PRIMARY);
        ok.addClickListener(buildSaveHandler());
        ok.focus();

        cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> this.close());

        footer.addComponent(ok);
        footer.addComponent(cancelButton);
        footer.setComponentAlignment(ok, Alignment.TOP_RIGHT);
        return footer;
    }

    @org.jetbrains.annotations.NotNull
    private ClickListener buildSaveHandler() {
        return event -> {
            // Valid base data first
            userBinder.validate();
            if (userBinder.isValid()) {
                // If password change is necessary or any of the password fields are entered, validate passwords too
                if (sessionUser.isPwChangeNecessary() ||
                        StringUtils.isNotEmpty(passwordData.getNewPassword()) ||
                        StringUtils.isNotEmpty(passwordData.getOldPassword()) ||
                        StringUtils.isNotEmpty(passwordData.getNewPasswordConfirmation())) {
                    passwordBinder.validate();
                    if (passwordBinder.isValid()) {
                        if (!cryptoService.checkPassword(sessionUser, passwordData.getOldPassword())) {
                            NotificationUtils.showError("Your old password doesn't match", false);
                        } else if (!Objects.equals(passwordData.getNewPassword(), passwordData.getNewPasswordConfirmation())) {
                            NotificationUtils.showError("Your new passwords don't match", false);
                        } else {
                            cryptoService.changePassword(sessionUser, passwordData.getNewPassword());
                            saveUserAndClose();
                        }
                    }
                } else {
                    saveUserAndClose();
                }
            }
        };
    }

    private void saveUserAndClose() {
        userService.save(sessionUser);
        NotificationUtils.showSuccess("Profile updated");
        close();
    }

}
