package org.xlrnet.datac.administration.ui.views.database;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.fields.MTextField;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.database.domain.DatabaseConnection;
import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.foundation.ui.components.AbstractEntityForm;
import org.xlrnet.datac.vcs.domain.Branch;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Simple editor component for deployment groups connections.
 */
@UIScope
@SpringComponent
public class AdminDeploymentInstanceForm extends AbstractEntityForm<DeploymentInstance> {
    // TODO: Refactor to use the same form as AdminDeploymentGroupForm

    /** Name of the group. */
    private final MTextField name = new MTextField("Instance name").withFullWidth();

    /** Read-only name of the parent group. */
    private final MTextField parentGroupName = new MTextField("Parent group").withReadOnly(true).withFullWidth();

    /** Branch which should be tracked by this instance.*/
    @Getter(AccessLevel.PROTECTED)
    private final ComboBox<Branch> branch = new ComboBox<>("Tracked branch");

    /** Database connection to use for this instance. */
    @Getter(AccessLevel.PROTECTED)
    private final ComboBox<DatabaseConnection> connection = new ComboBox<>("Database connection");

    @Autowired
    public AdminDeploymentInstanceForm() {
        super(DeploymentInstance.class);
    }

    @Override
    protected Component createContent() {
        MVerticalLayout content = new MVerticalLayout().withMargin(false);
        HorizontalLayout toolbar = getToolbar();
        branch.setWidth("100%");
        branch.setEmptySelectionAllowed(false);
        branch.setItemCaptionGenerator(Branch::getName);
        connection.setWidth("100%");
        connection.setEmptySelectionAllowed(false);
        connection.setItemCaptionGenerator(DatabaseConnection::getName);
        return content.with(parentGroupName, name, branch, connection, toolbar);
    }

    public void setAvailableConnections(Collection<DatabaseConnection> connections) {
        connection.setDataProvider(DataProvider.ofCollection(connections));
    }

    public void setAvailableBranches(Collection<Branch> branches) {
        branch.setDataProvider(DataProvider.ofCollection(branches));
    }

    @Override
    public void setEntity(DeploymentInstance entity) {
        super.setEntity(entity);
        String parentPath = entity.getGroup().getParentPath();
        String parentName = parentPath == null ? entity.getGroup().getName() : parentPath + "/" + entity.getGroup().getName();
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
