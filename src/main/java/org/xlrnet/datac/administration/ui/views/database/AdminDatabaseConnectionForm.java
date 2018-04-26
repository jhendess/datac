package org.xlrnet.datac.administration.ui.views.database;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.themes.ValoTheme;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.fields.EnumSelect;
import org.vaadin.viritin.fields.IntegerField;
import org.vaadin.viritin.fields.MTextField;
import org.vaadin.viritin.form.AbstractForm;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.ui.DatacTheme;
import org.xlrnet.datac.database.domain.DatabaseConnection;
import org.xlrnet.datac.database.domain.DatabaseType;

/**
 * Simple editor component for users.
 */
@UIScope
@SpringComponent
public class AdminDatabaseConnectionForm extends AbstractForm<DatabaseConnection> {

    /**
     * Login name of the entity.
     */
    private MTextField name = new MTextField("Connection name").withFullWidth();

    /**
     * First name of the entity.
     */
    private EnumSelect<DatabaseType> type = new EnumSelect<>("Database type", DatabaseType.class).withFullWidth();

    /**
     * Last name of the entity.
     */
    private MTextField host = new MTextField("Host").withFullWidth();

    /**
     * Port to connect to.
     */
    private IntegerField port = new IntegerField("Port").withFullWidth();

    /**
     * Database schema.
     */
    private MTextField schema = new MTextField("Schema").withFullWidth();

    /**
     * User for connecting to the database.
     */
    private MTextField user = new MTextField("Login user").withFullWidth();

    /**
     * Password of the login user.
     */
    private MTextField password = new MTextField("Password").withStyleName(DatacTheme.INPUT_PASSWORD);

    /**
     * JDBC URL of the connection.
     */
    private MTextField jdbcUrl = new MTextField("JDBC URL").withFullWidth();

    /**
     * Button to test connections. Logic must be injected by the caller.
     */
    @Getter
    private MButton testConnectionButton = new MButton("Test connection");

    @Getter
    private ProgressBar progressBar = new ProgressBar();

    @Autowired
    public AdminDatabaseConnectionForm() {
        super(DatabaseConnection.class);
    }

    @Override
    protected Component createContent() {
        HorizontalLayout toolbar = getToolbar();
        toolbar.addComponent(testConnectionButton);

        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        toolbar.addComponent(progressBar);

        password.setWidth("100%");
        name.selectAll();
        type.setEmptySelectionAllowed(false);

        return new MVerticalLayout(name, type, host, port, schema, user, password, jdbcUrl, toolbar).withMargin(false)
                .withStyleName(ValoTheme.FORMLAYOUT_LIGHT);
    }
}