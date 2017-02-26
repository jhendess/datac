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
import org.xlrnet.datac.foundation.ui.components.SimpleOkCancelWindow;
import org.xlrnet.datac.foundation.ui.views.AbstractSubview;
import org.xlrnet.datac.session.services.UserService;

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
     * Grid with all available users.
     */
    private Grid<User> grid;

    /**
     * Confirmation window for saving and deleting.
     */
    private SimpleOkCancelWindow confirmationWindow;

    @Autowired
    public AdminUserSubview(AdminUserForm editor, UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.editor = editor;
        this.userRepository = userRepository;
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
        editor.setCancelHandler(() -> editor.setVisible(false));

        layout.addComponent(newUserButton);
        layout.addComponent(editor);
        layout.addComponent(grid);

        updateUsers();

        return layout;
    }

    @NotNull
    private EntityChangeHandler<User> buildDeleteHandler() {
        return user -> {
            confirmationWindow.setCustomContent(new Label("Do you want to delete the user " + user.getLoginName() + "?<br>This action cannot be reverted!", ContentMode.HTML));
            confirmationWindow.setOkHandler(() -> {
                userService.delete(user);
                editor.setVisible(false);
                confirmationWindow.close();
                updateUsers();
            });

            UI.getCurrent().addWindow(confirmationWindow);
        };
    }

    @NotNull
    private EntityChangeHandler<User> buildSaveHandler() {
        return (user) -> {
            User existingUser = userRepository.findFirstByLoginNameIgnoreCase(user.getLoginName());
            if (user.getId() == null && existingUser != null) {
                WindowUtils.showModalDialog(null, "There is already a user with the same name.");
            } else {
                confirmationWindow.setCustomContent(new Label("Do you want to save the user?"));
                confirmationWindow.setOkHandler(() -> {
                    // TODO: Generate default password for new users
                    userService.createNewUser(user, "");
                    editor.setVisible(false);
                    confirmationWindow.close();
                    updateUsers();
                });

                UI.getCurrent().addWindow(confirmationWindow);
            }
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
}
