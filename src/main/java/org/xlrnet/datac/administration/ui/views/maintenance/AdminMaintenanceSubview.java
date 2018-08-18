package org.xlrnet.datac.administration.ui.views.maintenance;

import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import de.steinwedel.messagebox.MessageBox;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.label.MLabel;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.administration.services.ApplicationMaintenanceService;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.ui.NotificationUtils;
import org.xlrnet.datac.session.ui.views.AbstractSubview;

@UIScope
@SpringView(name = AdminMaintenanceSubview.VIEW_NAME)
public class AdminMaintenanceSubview extends AbstractSubview {

    public static final String VIEW_NAME = "admin/maintenance";

    @Autowired
    public AdminMaintenanceSubview(EventBus.ApplicationEventBus applicationEventBus, ApplicationMaintenanceService maintenanceService) {
        super(applicationEventBus, maintenanceService);
    }

    @NotNull
    @Override
    protected Component buildMainPanel() {
        MVerticalLayout layout = new MVerticalLayout();
        layout.add(buildActionLayout());
        return layout;
    }

    @Override
    protected void initialize() throws DatacTechnicalException {
        // No initialization necessary
    }

    @NotNull
    @Override
    protected String getSubtitle() {
        return "Perform critical maintenance work to ensure a fully working application.";
    }

    @NotNull
    @Override
    protected String getTitle() {
        return "Maintenance";
    }

    @NotNull
    private GridLayout buildActionLayout() {
        GridLayout layout = new GridLayout(2, 1);
        layout.addComponent(new MLabel("Recalculate all changeset checksums"));
        layout.addComponent(new MButton("Run").addClickListener(this::checkRecalculateChecksums));
        return layout;
    }

    private void checkRecalculateChecksums() {
        MessageBox.createWarning()
                .withCaption("Recalculate all changeset checksums")
                .withMessage("You're about to recalculate all changeset checksums.\n" +
                        "The application will enter maintenance mode and can't\n" +
                        "be used until the recalculation is finished.\n" +
                        "Do you really want to proceed?")
                .withNoButton()
                .withYesButton(this::startRecalculateChecksums)
                .open();
    }

    private void startRecalculateChecksums() {
        if (getMaintenanceService().startChecksumRecalculation()) {
            NotificationUtils.showSuccess("Started maintenance operation");
        } else {
            NotificationUtils.showError("Starting maintenance operation failed", false);
        }
    }
}
