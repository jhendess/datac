package org.xlrnet.datac.administration.ui.views.database;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.vaadin.viritin.form.AbstractForm;
import org.vaadin.viritin.grid.MGrid;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.ui.NotificationUtils;
import org.xlrnet.datac.database.domain.ConnectionPingResult;
import org.xlrnet.datac.database.domain.DatabaseConnection;
import org.xlrnet.datac.database.services.ConnectionManagerService;
import org.xlrnet.datac.database.services.DatabaseConnectionService;
import org.xlrnet.datac.database.services.DeploymentInstanceService;
import org.xlrnet.datac.database.tasks.CheckDatabaseConnectionTask;
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
     * Service for connecting to databases.
     */
    private final ConnectionManagerService connectionManagerService;

    /**
     * Service for accessing deployment instances.
     */
    private final DeploymentInstanceService instanceService;

    /**
     * Form for editing database connections.
     */
    private final AdminDatabaseConnectionForm dbForm;

    /**
     * Task executor.
     */
    private final TaskExecutor taskExecutor;

    @Autowired
    public AdminDatabaseSubview(DatabaseConnectionService connectionService, ConnectionManagerService connectionManagerService, DeploymentInstanceService instanceService, AdminDatabaseConnectionForm dbForm, @Qualifier("defaultTaskExecutor") TaskExecutor taskExecutor) {
        this.connectionService = connectionService;
        this.connectionManagerService = connectionManagerService;
        this.instanceService = instanceService;
        this.dbForm = dbForm;
        this.taskExecutor = taskExecutor;
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
        dbForm.setSavedHandler(this::saveConnection);
        /*dbForm.setCancelHandler(this::hideEditor);*/
        dbForm.getTestConnectionButton().addClickListener(this::checkConnectionForUser);

        mainLayout.with(grid).withExpand(grid, 0.75f);
        mainLayout.with(editorLayout).withExpand(editorLayout, 0.25f);

        updateConnections();

        return mainLayout;
    }

    private AbstractForm.DeleteHandler<DatabaseConnection> buildDeleteHandler() {
        return (connection -> {
            if (connection != null && connection.isPersisted()) {
                if (connection.getInstance() == null) {
                    connectionService.delete(connection);
                    NotificationUtils.showSaveSuccess();
                    hideEditor();
                } else {
                    NotificationUtils.showError("Deletion failed", String.format("The connection can't be deleted because it is bound to the project %s on instance %s", connection.getInstance().getGroup().getProject().getName(), connection.getInstance().getName()), true);
                }
            }
            updateConnections();
        });
    }

    private void saveConnection(DatabaseConnection databaseConnection) {
        CheckDatabaseConnectionTask task = new CheckDatabaseConnectionTask(dbForm.getEntity(), connectionManagerService);
        task.setEntityChangeHandler(pingResult -> {
            if (pingResult.isConnected()) {
                connectionService.save(databaseConnection);
                runOnUiThread(() -> {
                    hideEditor();
                    updateConnections();
                    NotificationUtils.showSaveSuccess();
                });
            } else {
                showConnectionError(pingResult);
            }
        });
        task.setRunningStatusHandler(this::setCheckingMode);
        taskExecutor.execute(task);
    }

    private void setCheckingMode(boolean checking) {
        runOnUiThread(() -> {
            dbForm.getProgressBar().setVisible(checking);
            dbForm.setEnabled(!checking);
        });
    }

    private void checkConnectionForUser() {
        CheckDatabaseConnectionTask task = new CheckDatabaseConnectionTask(dbForm.getEntity(), connectionManagerService);
        task.setEntityChangeHandler(pingResult -> {
            if (pingResult.isConnected()) {
                runOnUiThread(() -> NotificationUtils.showSuccess("Connection established"));
            } else {
                showConnectionError(pingResult);
            }
        });
        task.setRunningStatusHandler(this::setCheckingMode);
        taskExecutor.execute(task);
    }

    private void hideEditor() {
        dbForm.setVisible(false);
    }

    private void updateConnections() {
        grid.setItems(connectionService.findAllOrderByNameAsc());
    }

    private void showConnectionError(ConnectionPingResult pingResult) {
        runOnUiThread(() -> NotificationUtils.showError("Connection failed", pingResult.getException().getMessage(), false));
    }
}
