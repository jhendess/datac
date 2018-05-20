package org.xlrnet.datac.administration.ui.views.database;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.database.domain.DeploymentGroup;

/**
 * Simple editor component for deployment groups connections.
 */
@UIScope
@SpringComponent
public class AdminDeploymentGroupForm extends AbstractDeploymentForm<DeploymentGroup> {

    @Autowired
    public AdminDeploymentGroupForm() {
        super(DeploymentGroup.class);
    }

    @Override
    protected Component createContent() {
        getName().setCaption("Group name");
        MVerticalLayout content = new MVerticalLayout().withMargin(false);
        HorizontalLayout toolbar = getToolbar();
        return content.with(getParentGroupName(), getName(), getBranch(), toolbar);
    }

    @NotNull
    @Override
    String determineParentGroupName(@NotNull DeploymentGroup entity) {
        return entity.getParent() != null ? entity.getParentPath() : "";
    }
}
