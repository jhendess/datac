package org.xlrnet.datac.administration.ui.views.database;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.database.domain.DeploymentGroup;
import org.xlrnet.datac.database.services.DeploymentGroupService;
import org.xlrnet.datac.foundation.services.ValidationService;

/**
 * Simple editor component for deployment groups connections.
 */
@UIScope
@SpringComponent
public class AdminDeploymentGroupForm extends AbstractDeploymentForm<DeploymentGroup, DeploymentGroupService> {

    @Autowired
    public AdminDeploymentGroupForm(ValidationService validationService, DeploymentGroupService transactionalService) {
        super(DeploymentGroup.class, transactionalService, validationService);
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
