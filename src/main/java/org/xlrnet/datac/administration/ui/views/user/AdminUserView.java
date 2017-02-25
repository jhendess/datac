package org.xlrnet.datac.administration.ui.views.user;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.administration.domain.User;
import org.xlrnet.datac.administration.repository.UserRepository;
import org.xlrnet.datac.foundation.ui.Subview;

import javax.annotation.PostConstruct;

/**
 * Admin view which is responsible for managing the available users.
 */
@SpringView(name = AdminUserView.VIEW_NAME)
public class AdminUserView extends VerticalLayout implements Subview {

    public static final String VIEW_NAME = "admin/users";

    private final UserRepository userRepository;

    private final UserEditor editor;

    private Grid<User> grid;

    @Autowired
    public AdminUserView(UserRepository userRepository, UserEditor editor) {
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
        Label infoText = new Label("Create new users or modify existing ones. To modify a property, " +
                "just double-click into the cell and hit save when you're finished.");

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
            editor.editUser(new User());
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
        grid.asSingleSelect().addValueChangeListener(e -> editor.editUser(e.getValue()));

        // Listen changes made by the editor, refresh data from backend
        editor.setChangeHandler(() -> {
            editor.setVisible(false);
            updateUsers();
        });

        layout.addComponent(newUserButton);
        layout.addComponent(editor);
        layout.addComponent(grid);

        updateUsers();

        return layout;
    }

    private void updateUsers() {
        grid.setItems(userRepository.findAllByOrderByLoginNameAsc());
    }
}
