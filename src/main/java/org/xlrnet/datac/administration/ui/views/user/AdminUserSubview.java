package org.xlrnet.datac.administration.ui.views.user;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.grid.MGrid;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.ui.NotificationUtils;
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

import de.steinwedel.messagebox.MessageBox;

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
        MVerticalLayout editorLayout = new MVerticalLayout().withMargin(false);
        editorLayout.with(newUserButton);
        editorLayout.with(defaultPasswordTextField);
        editorLayout.with(userForm);
        userForm.setVisible(false);

        grid = new MGrid<>();
        grid.withStyleName("editor-list-grid").withFullWidth();
        grid.addColumn(User::getLoginName).setCaption("Login");
        grid.addColumn(User::getFirstName).setCaption("First name");
        grid.addColumn(User::getLastName).setCaption("Last name");
        grid.addColumn(User::getEmail).setCaption("Email");

        // Select the user in the editor when clicked
        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                userForm.setEntity(userService.refresh(e.getValue()));
            }
        });

        // Setup all handlers
        userForm.setSavedHandler(this::savedHandler);
        userForm.setDeleteHandler(this::deleteHandler);
        userForm.setResetHandler((x) -> hideEditor());
        userForm.setMessageGenerator((u) -> String.format("Do you want to delete the user %s?\nThis action cannot be reverted!", u.getLoginName()));

        mainLayout.with(grid).withExpand(grid, 0.75f);
        mainLayout.with(editorLayout).withExpand(editorLayout, 0.25f);

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

    private void deleteHandler(User user) {
        if (Objects.equals(user.getId(), userService.getSessionUser().getId())) {
            MessageBox.createWarning().withMessage("You cannot delete your own user!").open();
        } else {
            userService.delete(user);
            hideEditor();
            updateUsers();
            NotificationUtils.showSaveSuccess();
        }
    }

    private void savedHandler(User user) {
            User existingUser = userService.findFirstByLoginNameIgnoreCase(user.getLoginName());
            // If user with same login name exists and who has a different ID than the current -> reject creation
            if (existingUser != null && !Objects.equals(existingUser.getId(), user.getId())) {
                NotificationUtils.showError("There is already a user with the same name.", true);
            } else {
                StringBuilder saveConfirmation = new StringBuilder("Do you want to save the user?");
                if (user.getId() == null) {
                    saveConfirmation.append("<br>The default password is <strong>").append(defaultPassword).append("</strong>");
                }
                MessageBox.createQuestion()
                        .withCaption("Create new user")
                        .withHtmlMessage(saveConfirmation.toString())
                        .withYesButton(() -> persistUser(user))
                        .withNoButton()
                        .open();
            }
    }

    private void persistUser(User user) {
        if (user.getId() == null) {
            user.setPwChangeNecessary(true);
            userService.createNewUser(user, defaultPassword);
        } else {
            userService.save(user);
        }
        hideEditor();
        updateUsers();
        NotificationUtils.showSaveSuccess();
    }

    private void updateUsers() {
        grid.setItems(userService.findAllByOrderByLoginNameAsc());
    }

    private void hideEditor() {
        defaultPasswordTextField.setVisible(false);
        userForm.setVisible(false);
    }
}
