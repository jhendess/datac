package org.xlrnet.datac.administration.ui.views.database;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.database.domain.DatabaseConnection;
import org.xlrnet.datac.database.domain.DeploymentInstance;

import java.util.Collection;

/**
 * Simple editor component for deployment groups connections.
 */
@UIScope
@SpringComponent
public class AdminDeploymentInstanceForm extends AbstractDeploymentForm<DeploymentInstance> {

    /** Database connection to use for this instance. */
    @Getter(AccessLevel.PROTECTED)
    private final ComboBox<DatabaseConnection> connection = new ComboBox<>("Database connection");

    @Autowired
    public AdminDeploymentInstanceForm() {
        super(DeploymentInstance.class);
        connection.setWidth("100%");
        connection.setEmptySelectionAllowed(false);
        connection.setItemCaptionGenerator(DatabaseConnection::getName);
    }

    @Override
    protected Component createContent() {
        getName().setCaption("Instance name");
        MVerticalLayout content = new MVerticalLayout().withMargin(false);
        HorizontalLayout toolbar = getToolbar();
        return content.with(getParentGroupName(), getName(), getBranch(), connection, toolbar);
    }

    void setAvailableConnections(Collection<DatabaseConnection> connections) {
        connection.setDataProvider(DataProvider.ofCollection(connections));
    }

    @NotNull
    String determineParentGroupName(@NotNull DeploymentInstance entity) {
        String parentPath = entity.getGroup().getParentPath();
        return parentPath == null ? entity.getGroup().getName() : parentPath + "/" + entity.getGroup().getName();
    }
}
