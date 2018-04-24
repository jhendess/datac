package org.xlrnet.datac.administration.ui.views.database;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.form.AbstractForm;
import org.vaadin.viritin.grid.MGrid;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.ui.NotificationUtils;
import org.xlrnet.datac.database.domain.DatabaseConnection;
import org.xlrnet.datac.database.services.DatabaseConnectionService;
import org.xlrnet.datac.session.ui.views.AbstractSubview;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;

@SpringComponent
@SpringView(name = AdminDatabaseSubview.VIEW_NAME)
public class AdminDatabaseSubview extends AbstractSubview {

    public static final String VIEW_NAME = "admin/database";

    /**
     * Grid with all available connections.
     */
    private MGrid<DatabaseConnection> grid;

    /**
     * Service for accessing database connections.
     **/
    private final DatabaseConnectionService connectionService;

    /**
     * Form for editing database connections.
     */
    private final AdminDatabaseConnectionForm dbForm;

    @Autowired
    public AdminDatabaseSubview(DatabaseConnectionService connectionService, AdminDatabaseConnectionForm dbForm) {
        this.connectionService = connectionService;
        this.dbForm = dbForm;
    }

    @Override
    protected void initialize() throws DatacTechnicalException {
        // Nothing to do
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        return "Configure available database connections";
    }

    @NotNull
    @Override
    protected String getTitle() {
        return "Database connection administration";
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        MHorizontalLayout mainLayout = new MHorizontalLayout().withFullSize();

        Button newConnectionButton = new Button("New");
        newConnectionButton.setIcon(VaadinIcons.PLUS);
        newConnectionButton.addClickListener(e -> {
            dbForm.setEntity(new DatabaseConnection());
            dbForm.setDeleteHandler(null);  // Disable delete button by removing the handler
            dbForm.setVisible(true);
        });
        MVerticalLayout editorLayout = new MVerticalLayout().withMargin(false).withStyleName("editor-list-form");
        editorLayout.with(newConnectionButton);
        editorLayout.with(dbForm);
        dbForm.setVisible(false);   // Invisible by default

        grid = new MGrid<>();
        grid.withStyleName("editor-list-grid").withFullWidth();
        grid.addColumn(DatabaseConnection::getName).setCaption("Name");
        grid.addColumn(DatabaseConnection::getType).setCaption("Type");
        grid.addColumn(DatabaseConnection::getHost).setCaption("Host");
        grid.addColumn(DatabaseConnection::getPort).setCaption("Port");
        grid.addColumn(DatabaseConnection::getSchema).setCaption("Schema");
        grid.addColumn(DatabaseConnection::getJdbcUrl).setCaption("Connection URL");

        // Select the database in the editor when clicked
        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                dbForm.setEntity(connectionService.refresh(e.getValue()));
                dbForm.setDeleteHandler(buildDeleteHandler());
            } else {
                hideEditor();
            }
        });

        // Setup all handlers
        dbForm.setSavedHandler(buildSavedHandler());
        /*dbForm.setCancelHandler(this::hideEditor);*/


        mainLayout.with(editorLayout).withExpand(editorLayout, 0.25f);
        mainLayout.with(grid).withExpand(grid, 0.75f);

        updateConnections();

        return mainLayout;
    }

    private AbstractForm.DeleteHandler<DatabaseConnection> buildDeleteHandler() {
        return (connection -> {
            if (connection != null && connection.isPersisted()) {
                connectionService.delete(connection);
                NotificationUtils.showSaveSuccess();
            }
            updateConnections();
            hideEditor();
        });
    }

    private AbstractForm.SavedHandler<DatabaseConnection> buildSavedHandler() {
        return (databaseConnection -> {
            connectionService.save(databaseConnection);
            hideEditor();
            updateConnections();
            NotificationUtils.showSaveSuccess();
        });
    }

    private void hideEditor() {
        dbForm.setVisible(false);
    }

    private void updateConnections() {
        grid.setItems(connectionService.findAllOrderByNameAsc());
        System.out.println("bla");
    }
}
