package org.xlrnet.datac.administration.ui.views.user;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.grid.MGrid;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.ui.NotificationUtils;
import org.xlrnet.datac.commons.util.WindowUtils;
import org.xlrnet.datac.foundation.ui.components.EntityChangeHandler;
import org.xlrnet.datac.foundation.ui.components.GenericHandler;
import org.xlrnet.datac.foundation.ui.components.SimpleOkCancelWindow;
import org.xlrnet.datac.session.domain.User;
import org.xlrnet.datac.session.services.CryptoService;
import org.xlrnet.datac.session.services.UserService;
import org.xlrnet.datac.session.ui.views.AbstractSubview;
import org.xlrnet.datac.session.ui.views.Subview;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

/**
 * Admin view which is responsible for managing the available users.
 */
@SpringComponent
@SpringView(name = AdminUserSubview.VIEW_NAME)
public class AdminUserSubview extends AbstractSubview implements Subview {

    public static final String VIEW_NAME = "admin/users";

    /**
     * Editor component for users.
     */
    private final AdminUserForm userForm;

    /**
     * User service containing business logic for managing users.
     */
    private final UserService userService;

    /**
     * Service for generating passwords.
     */
    private final CryptoService cryptoService;

    /**
     * Text field informing the user about the default.
     */
    private Label defaultPasswordTextField = new Label("", ContentMode.HTML);

    /**
     * Default password for new user.
     */
    private String defaultPassword;

    /**
     * Grid with all available users.
     */
    private MGrid<User> grid;

    /**
     * Confirmation window for saving and deleting.
     */
    private SimpleOkCancelWindow confirmationWindow;

    @Autowired
    public AdminUserSubview(AdminUserForm userForm, UserService userService, CryptoService cryptoService) {
        this.userService = userService;
        this.userForm = userForm;
        this.cryptoService = cryptoService;
    }

    @Override
    protected void initialize() {
        // Nothing to do
    }

    @Override
    @NotNull
    protected String getSubtitle() {
        return "Configure who can access the application. Click on an existing user to modify him.";
    }

    @Override
    @NotNull
    protected String getTitle() {
        return "User administration";
    }

    @Override
    @NotNull
    protected Component buildMainPanel() {
        MHorizontalLayout mainLayout = new MHorizontalLayout().withFullSize();

        Button newUserButton = new Button("New");
        newUserButton.setIcon(VaadinIcons.PLUS);
        newUserButton.addClickListener(e -> {
            generateDefaultPassword();
            userForm.setEntity(new User());
            userForm.setVisible(true);
        });
        MVerticalLayout editorLayout = new MVerticalLayout().withMargin(false).withStyleName("editor-list-form");
        editorLayout.with(newUserButton);
        editorLayout.with(defaultPasswordTextField);
        editorLayout.with(userForm);

        grid = new MGrid<>();
        grid.withStyleName("editor-list-grid").withFullWidth();
        grid.addColumn(User::getLoginName).setCaption("Login");
        grid.addColumn(User::getFirstName).setCaption("First name");
        grid.addColumn(User::getLastName).setCaption("Last name");
        grid.addColumn(User::getEmail).setCaption("Email");

        // Select the user in the editor when clicked
        grid.asSingleSelect().addValueChangeListener(e -> userForm.setEntity(userService.refresh(e.getValue())));

        // Prepare confirmation window
        confirmationWindow = new SimpleOkCancelWindow();

        // Setup all handlers
        userForm.setSaveHandler(buildSaveHandler());
        userForm.setDeleteHandler(buildDeleteHandler());
        userForm.setCancelHandler(this::hideEditor);


        mainLayout.with(editorLayout).withExpand(editorLayout, 0.25f);
        mainLayout.with(grid).withExpand(grid, 0.75f);

        updateUsers();

        return mainLayout;
    }

    private void generateDefaultPassword() {
        // Generate a too short password to force the user on his first login to change it
        defaultPassword = cryptoService.generateUserPassword(CryptoService.MINIMUM_PASSWORD_SIZE);
        defaultPasswordTextField.setValue("The new user will be created using the <br>password <strong>" + defaultPassword + "</strong>." +
                "The password has to be<br>changed after the first login.");
        defaultPasswordTextField.setVisible(true);
    }

    @NotNull
    private EntityChangeHandler<User> buildDeleteHandler() {
        return user -> {
            if (Objects.equals(user.getId(), userService.getSessionUser().getId())) {
                WindowUtils.showModalDialog("", "You cannot delete your own user!");
            } else {
                confirmationWindow.setCustomContent(new Label("Do you want to delete the user " + user.getLoginName() + "?<br>This action cannot be reverted!", ContentMode.HTML));
                confirmationWindow.setOkHandler(() -> {
                    userService.delete(user);
                    hideEditor();
                    confirmationWindow.close();
                    updateUsers();
                    NotificationUtils.showTrayNotification("User deleted successfully");
                });

                UI.getCurrent().addWindow(confirmationWindow);
            }
        };
    }

    @NotNull
    private EntityChangeHandler<User> buildSaveHandler() {
        return (user) -> {
            User existingUser = userService.findFirstByLoginNameIgnoreCase(user.getLoginName());
            // If user with same login name exists and who has a different ID than the current -> reject creation
            if (existingUser != null && !Objects.equals(existingUser.getId(), user.getId())) {
                WindowUtils.showModalDialog(null, "There is already a user with the same name.");
            } else {
                StringBuilder saveConfirmation = new StringBuilder("Do you want to save the user?");
                if (user.getId() == null) {
                    saveConfirmation.append("<br>The default password is <strong>").append(defaultPassword).append("</strong>");
                }
                confirmationWindow.setCustomContent(new Label(saveConfirmation.toString(), ContentMode.HTML));
                confirmationWindow.setOkHandler(buildPersistUserHandler(user));
                UI.getCurrent().addWindow(confirmationWindow);
            }
        };
    }

    @NotNull
    private GenericHandler buildPersistUserHandler(User user) {
        return () -> {
            if (user.getId() == null) {
                user.setPwChangeNecessary(true);
                userService.createNewUser(user, defaultPassword);
            } else {
                userService.save(user);
            }
            hideEditor();
            confirmationWindow.close();
            updateUsers();
            Notification.show("User saved successfully", Notification.Type.TRAY_NOTIFICATION);
        };
    }

    private void updateUsers() {
        grid.setItems(userService.findAllByOrderByLoginNameAsc());
    }

    private void hideEditor() {
        defaultPasswordTextField.setVisible(false);
        userForm.setVisible(false);
    }
}
