package org.xlrnet.datac.administration.ui.views.user;

import org.jetbrains.annotations.NotNull;
import org.vaadin.viritin.fields.MTextField;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.foundation.ui.components.AbstractForm;
import org.xlrnet.datac.session.domain.User;

import com.vaadin.data.BeanValidationBinder;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Simple editor component for users.
 */
@UIScope
@SpringComponent
public class AdminUserForm extends AbstractForm<User> {

    /**
     * Login name of the entity.
     */
    private MTextField loginName = new MTextField("Login").withFullWidth();

    /**
     * First name of the entity.
     */
    private MTextField firstName = new MTextField("First name").withFullWidth();

    /**
     * Last name of the entity.
     */
    private MTextField lastName = new MTextField("Last name").withFullWidth();

    /**
     * Email of the entity.
     */
    private MTextField email = new MTextField("E-Mail address").withFullWidth();

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
        return new MVerticalLayout(loginName, firstName, lastName, email).withMargin(false)
                .withStyleName(ValoTheme.FORMLAYOUT_LIGHT);
    }
}