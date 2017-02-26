package org.xlrnet.datac.administration.ui.views.user;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.administration.domain.User;
import org.xlrnet.datac.administration.repository.UserRepository;
import org.xlrnet.datac.foundation.ui.Subview;
import org.xlrnet.datac.foundation.ui.components.AdminUserForm;
import org.xlrnet.datac.foundation.ui.components.SimpleOkCancelWindow;

import javax.annotation.PostConstruct;

/**
 * Admin view which is responsible for managing the available users.
 */
@SpringView(name = AdminUserView.VIEW_NAME)
public class AdminUserView extends VerticalLayout implements Subview {

    public static final String VIEW_NAME = "admin/users";

    private final UserRepository userRepository;

    private final AdminUserForm editor;

    private Grid<User> grid;

    private SimpleOkCancelWindow confirmationWindow;

    @Autowired
    public AdminUserView(UserRepository userRepository, AdminUserForm editor) {
        this.userRepository = userRepository;
        this.editor = editor;
    }

    @NotNull
    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        // No action necessary
    }

    @PostConstruct
    private void init() {
        Component topPanel = buildGreetingPanel();
        Component editPanel = buildEditPanel();

        addComponent(topPanel);
        addComponent(editPanel);
    }

    @NotNull
    private Component buildGreetingPanel() {
        VerticalLayout topPanel = new VerticalLayout();
        topPanel.setSpacing(false);
        topPanel.setMargin(false);

        Label title = new Label("User management");
        title.setStyleName(ValoTheme.LABEL_H1);
        Label infoText = new Label("Create new users or modify existing ones. Click on an existing user to modify him.");

        topPanel.addComponent(title);
        topPanel.addComponent(infoText);
        return topPanel;
    }

    @NotNull
    private Component buildEditPanel() {
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

        // Listen changes made by the editor, refresh data from backend
        editor.setSaveHandler((user) -> {
            confirmationWindow.setCustomContent(new Label("Do you want to save the new user?"));
            confirmationWindow.setOkHandler(() -> {
                userRepository.save(user);
                editor.setVisible(false);
                confirmationWindow.close();
                updateUsers();
            });

            UI.getCurrent().addWindow(confirmationWindow);
        });

        editor.setDeleteHandler(user -> {
            confirmationWindow.setCustomContent(new Label("Do you want to delete the user " + user.getLoginName() + "?<br>This action cannot be reverted!", ContentMode.HTML));
            confirmationWindow.setOkHandler(() -> {
                userRepository.delete(user);
                editor.setVisible(false);
                confirmationWindow.close();
                updateUsers();
            });

            UI.getCurrent().addWindow(confirmationWindow);
        });

        editor.setCancelHandler(() -> editor.setVisible(false));

        layout.addComponent(newUserButton);
        layout.addComponent(editor);
        layout.addComponent(grid);

        updateUsers();

        return layout;
    }

    @Nullable
    private User reloadEntity(@Nullable User value) {
        // Find fresh entity for editing
        if (value != null) {
            return userRepository.findOne(value.getId());
        } else {
            return null;
        }
    }

    private void updateUsers() {
        grid.setItems(userRepository.findAllByOrderByLoginNameAsc());
    }
}
