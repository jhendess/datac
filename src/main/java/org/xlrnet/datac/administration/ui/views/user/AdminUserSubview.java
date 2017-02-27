package org.xlrnet.datac.administration.ui.views.user;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.administration.domain.User;
import org.xlrnet.datac.administration.repository.UserRepository;
import org.xlrnet.datac.commons.util.WindowUtils;
import org.xlrnet.datac.foundation.ui.Subview;
import org.xlrnet.datac.foundation.ui.components.EntityChangeHandler;
import org.xlrnet.datac.foundation.ui.components.GenericHandler;
import org.xlrnet.datac.foundation.ui.components.SimpleOkCancelWindow;
import org.xlrnet.datac.foundation.ui.views.AbstractSubview;
import org.xlrnet.datac.session.services.PasswordService;
import org.xlrnet.datac.session.services.UserService;

import java.util.Objects;

/**
 * Admin view which is responsible for managing the available users.
 */
@SpringView(name = AdminUserSubview.VIEW_NAME)
public class AdminUserSubview extends AbstractSubview implements Subview {

    public static final String VIEW_NAME = "admin/users";

    /**
     * Editor component for users.
     */
    private final AdminUserForm editor;

    /**
     * User service containing business logic for managing users.
     */
    private final UserService userService;

    /**
     * Data access for users.
     */
    private final UserRepository userRepository;

    /**
     * Service for generating passwords.
     */
    private final PasswordService passwordService;

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
    private Grid<User> grid;

    /**
     * Confirmation window for saving and deleting.
     */
    private SimpleOkCancelWindow confirmationWindow;

    @Autowired
    public AdminUserSubview(AdminUserForm editor, UserService userService, UserRepository userRepository, PasswordService passwordService) {
        this.userService = userService;
        this.editor = editor;
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @Override
    @NotNull
    protected String getSubtitle() {
        return "Create new users or modify existing ones. Click on an existing user to modify him.";
    }

    @Override
    @NotNull
    protected String getTitle() {
        return "User management";
    }

    @Override
    @NotNull
    protected Component buildMainPanel() {
        VerticalLayout layout = new VerticalLayout();
        Button newUserButton = new Button("New");
        newUserButton.setIcon(VaadinIcons.PLUS);
        newUserButton.addClickListener(e -> {
            generateDefaultPassword();
            editor.setEntity(new User());
            editor.setVisible(true);
        });

        grid = new Grid<>();
        grid.setSizeFull();
        grid.addColumn(User::getId).setCaption("ID");
        grid.addColumn(User::getLoginName).setCaption("Login");
        grid.addColumn(User::getFirstName).setCaption("First name");
        grid.addColumn(User::getLastName).setCaption("Last name");
        grid.addColumn(User::getEmail).setCaption("Email");

        // Select the user in the editor when clicked
        grid.asSingleSelect().addValueChangeListener(e -> editor.setEntity(reloadEntity(e.getValue())));

        // Prepare confirmation window
        confirmationWindow = new SimpleOkCancelWindow();

        // Setup all handlers
        editor.setSaveHandler(buildSaveHandler());
        editor.setDeleteHandler(buildDeleteHandler());
        editor.setCancelHandler(this::hideEditor);

        layout.addComponent(newUserButton);
        layout.addComponent(defaultPasswordTextField);
        layout.addComponent(editor);
        layout.addComponent(grid);

        updateUsers();

        return layout;
    }

    private void generateDefaultPassword() {
        // Generate a too short password to force the user on his first login to change it
        defaultPassword = passwordService.generatePassword(PasswordService.MINIMUM_PASSWORD_SIZE);
        defaultPasswordTextField.setValue("The new user will be created using the password <strong>" + defaultPassword + "</strong><br>" +
                "The password has to be changed after the first login.");
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
                    Notification.show("User deleted successfully", Notification.Type.TRAY_NOTIFICATION);
                });

                UI.getCurrent().addWindow(confirmationWindow);
            }
        };
    }

    @NotNull
    private EntityChangeHandler<User> buildSaveHandler() {
        return (user) -> {
            User existingUser = userRepository.findFirstByLoginNameIgnoreCase(user.getLoginName());
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
                userRepository.save(user);
            }
            hideEditor();
            confirmationWindow.close();
            updateUsers();
            Notification.show("User saved successfully", Notification.Type.TRAY_NOTIFICATION);
        };
    }

    @Nullable
    private User reloadEntity(@Nullable User value) {
        // Find fresh entity for editing
        if (value != null) {
            return userService.findOne(value.getId());
        } else {
            return null;
        }
    }

    private void updateUsers() {
        grid.setItems(userRepository.findAllByOrderByLoginNameAsc());
    }

    private void hideEditor() {
        defaultPasswordTextField.setVisible(false);
        editor.setVisible(false);
    }
}
