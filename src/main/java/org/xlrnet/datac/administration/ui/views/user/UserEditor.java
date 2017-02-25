package org.xlrnet.datac.administration.ui.views.user;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.event.ShortcutAction;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.xlrnet.datac.administration.domain.User;
import org.xlrnet.datac.administration.repository.UserRepository;

/**
 * Simple editor component for users.
 * <p>
 * In a real world application you'll most likely using a common super class for all your
 * forms - less code, better UX. See e.g. AbstractForm in Viritin
 * (https://vaadin.com/addon/viritin).
 */
@SpringComponent
@UIScope
public class UserEditor extends FormLayout {

    private final UserRepository repository;

    /** The currently edited entity. */
    private User user;

    /** Login name of the user. */
    private TextField loginName = new TextField("Login");

    /** First name of the user. */
    private TextField firstName = new TextField("First name");

    /** Last name of the user. */
    private TextField lastName = new TextField("Last name");

    /** Email of the user. */
    private TextField email = new TextField("E-Mail address");

    /** Save action button. */
    private Button save = new Button("Save", VaadinIcons.CHECK);

    /** Cancel action button. */
    private Button cancel = new Button("Cancel");
    
    /** Delete action button. */
    private Button delete = new Button("Delete", VaadinIcons.TRASH);

    /** Binds fields in this class to fields in the User class when values are changed. */
    private BeanValidationBinder<User> binder = new BeanValidationBinder<>(User.class);

    @Autowired
    public UserEditor(UserRepository repository) {
        this.repository = repository;

        HorizontalLayout fields = new HorizontalLayout(loginName, firstName, lastName, email);
        CssLayout actions = new CssLayout(save, cancel, delete);
        addComponents(fields, actions);

        // Bind text fields to actual bean properties with the same name
        binder.bindInstanceFields(this);

        // Configure and style components
        setSpacing(true);
        actions.setStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        save.setStyleName(ValoTheme.BUTTON_PRIMARY);
        save.setClickShortcut(ShortcutAction.KeyCode.ENTER);

        // wire action buttons to save, delete and reset
        save.addClickListener(e -> {
            binder.validate();
            if (binder.isValid()) {
                repository.save(user);
            }
        });
        delete.addClickListener(e -> repository.delete(user));
        cancel.addClickListener(e -> editUser(null));
        setVisible(false);
    }

    public interface ChangeHandler {
        void onChange();
    }

    final void editUser(User c) {
        if (c == null) {
            setVisible(false);
            return;
        }
        final boolean persisted = c.getId() != null;
        if (persisted) {
            // Find fresh entity for editing
            user = repository.findOne(c.getId());
        } else {
            user = c;
        }
        cancel.setVisible(persisted);

        binder.setBean(user);
        setVisible(true);

        // A hack to ensure the whole form is visible
        save.focus();
        // Select all text in firstName field automatically
        loginName.selectAll();
    }

     void setChangeHandler(ChangeHandler h) {
        // ChangeHandler is notified when either save or delete is clicked - the save handler will only be run if the field is valid
        save.addClickListener(e -> {
            if (binder.isValid()) {
                h.onChange();
            }
        });
        delete.addClickListener(e -> h.onChange());
    }

}