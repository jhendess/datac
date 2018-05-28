package org.xlrnet.datac.administration.ui.views.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.fields.MTextField;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.foundation.services.ValidationService;
import org.xlrnet.datac.foundation.ui.components.AbstractEntityForm;
import org.xlrnet.datac.session.domain.User;
import org.xlrnet.datac.session.services.UserService;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Simple editor component for users.
 */
@UIScope
@SpringComponent
public class AdminUserForm extends AbstractEntityForm<User, UserService> {

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

    @Autowired
    public AdminUserForm(ValidationService validationService, UserService transactionalService) {
        super(User.class, transactionalService, validationService);
    }

    @Override
    protected Component createContent() {
        // Select all text in firstName field automatically
        loginName.selectAll();
        return new MVerticalLayout(loginName, firstName, lastName, email, getToolbar()).withMargin(false).withStyleName(ValoTheme.FORMLAYOUT_LIGHT);
    }

    @Override
    protected void postSave() {
        // Don't show the notification from the superclass
    }
}