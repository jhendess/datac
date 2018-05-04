package org.xlrnet.datac.administration.ui.views.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.fields.MTextField;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.database.domain.DeploymentGroup;
import org.xlrnet.datac.foundation.ui.components.AbstractEntityForm;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;

/**
 * Simple editor component for deployment groups connections.
 */
@UIScope
@SpringComponent
public class AdminDeploymentGroupForm extends AbstractEntityForm<DeploymentGroup> {

    /** Name of the group. */
    private final MTextField name = new MTextField("Group name").withFullWidth();

    /** Read-only name of the parent group. */
    private final MTextField parentGroupName = new MTextField("Parent group").withReadOnly(true).withFullWidth();

    @Autowired
    public AdminDeploymentGroupForm() {
        super(DeploymentGroup.class);
    }

    @Override
    protected Component createContent() {
        MVerticalLayout content = new MVerticalLayout().withMargin(false);
        HorizontalLayout toolbar = getToolbar();
        return content.with(parentGroupName, name, toolbar);
    }

    @Override
    public void setEntity(DeploymentGroup entity) {
        super.setEntity(entity);
        String parentName = entity.getParent() != null ? entity.getParentPath() : "";
        parentGroupName.setValue(parentName);
    }

    @Override
    protected void adjustResetButtonState() {
        if (getPopup() != null && getPopup().getParent() != null) {
            // Assume cancel button in a form opened to a popup also closes
            // it, allows closing via cancel button by default
            getResetButton().setEnabled(true);
            return;
        }
        if (isBound()) {
            // Always show the cancel button for this form
            getResetButton().setEnabled(true);
        }
    }
}
