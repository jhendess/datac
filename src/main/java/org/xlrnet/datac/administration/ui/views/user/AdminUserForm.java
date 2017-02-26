package org.xlrnet.datac.administration.ui.views.user;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.administration.domain.User;
import org.xlrnet.datac.foundation.ui.components.AbstractForm;

/**
 * Simple editor component for users.
 * <p>
 * In a real world application you'll most likely using a common super class for all your
 * forms - less code, better UX. See e.g. AbstractForm in Viritin
 * (https://vaadin.com/addon/viritin).
 */
@UIScope
@SpringComponent
public class AdminUserForm extends AbstractForm<User> {

    /**
     * Login name of the entity.
     */
    private TextField loginName = new TextField("Login");

    /**
     * First name of the entity.
     */
    private TextField firstName = new TextField("First name");

    /**
     * Last name of the entity.
     */
    private TextField lastName = new TextField("Last name");

    /**
     * Email of the entity.
     */
    private TextField email = new TextField("E-Mail address");

    @Override
    @NotNull
    protected BeanValidationBinder<User> buildBinder() {
        return new BeanValidationBinder<>(User.class);
    }

    @Override
    @NotNull
    protected Component getContent() {
        // Select all text in firstName field automatically
        loginName.selectAll();
        return new HorizontalLayout(loginName, firstName, lastName, email);
    }
}