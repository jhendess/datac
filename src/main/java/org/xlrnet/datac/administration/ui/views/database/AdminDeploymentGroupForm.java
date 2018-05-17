package org.xlrnet.datac.administration.ui.views.database;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.fields.MTextField;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.ui.DatacTheme;
import org.xlrnet.datac.database.domain.DeploymentGroup;
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
public class AdminDeploymentGroupForm extends AbstractEntityForm<DeploymentGroup> {

    /** Name of the group. */
    private final MTextField name = new MTextField("Group name").withFullWidth();

    /** Read-only name of the parent group. */
    private final MTextField parentGroupName = new MTextField("Parent group").withReadOnly(true).withFullWidth();

    /** Optional branch which should be tracked by this group.*/
    @Getter(AccessLevel.PROTECTED)
    private final ComboBox<Branch> branch = new ComboBox<>("Tracked branch");

    @Autowired
    public AdminDeploymentGroupForm() {
        super(DeploymentGroup.class);
    }

    @Override
    protected Component createContent() {
        MVerticalLayout content = new MVerticalLayout().withMargin(false);
        HorizontalLayout toolbar = getToolbar();
        branch.setWidth(DatacTheme.FULL_SIZE);
        branch.setEmptySelectionAllowed(false);
        branch.setItemCaptionGenerator(Branch::getName);
        return content.with(parentGroupName, name, branch, toolbar);
    }

    @Override
    public void setEntity(DeploymentGroup entity) {
        super.setEntity(entity);
        String parentName = entity.getParent() != null ? entity.getParentPath() : "";
        parentGroupName.setValue(parentName);
    }

    public void setAvailableBranches(Collection<Branch> branches) {
        branch.setDataProvider(DataProvider.ofCollection(branches));
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
