package org.xlrnet.datac.session.ui.components;

import com.vaadin.event.ShortcutAction;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Created by jhendess on 24.02.2017.
 */
public class CustomLoginForm extends LoginForm {

    @Override
    protected Component createContent(TextField userNameField, PasswordField passwordField, Button loginButton) {
        HorizontalLayout layout = new HorizontalLayout();

        userNameField.setIcon(VaadinIcons.USER);
        userNameField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        userNameField.focus();

        passwordField.setIcon(VaadinIcons.LOCK);
        passwordField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);

        loginButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        loginButton.setClickShortcut(ShortcutAction.KeyCode.ENTER);

        layout.addComponents(userNameField, passwordField, loginButton);
        layout.setComponentAlignment(loginButton, Alignment.BOTTOM_RIGHT);

        layout.addStyleName("fields");

        return layout;
    }


}
