package org.xlrnet.datac.administration.services;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.foundation.ui.components.BooleanStatusChangeHandler;
import org.xlrnet.datac.foundation.ui.components.ProgressChangeHandler;

@Component
@Scope("singleton")
public class MaintenanceOperationStatusObserver implements ProgressChangeHandler, BooleanStatusChangeHandler {

    private final ApplicationMaintenanceService applicationMaintenanceService;

    public MaintenanceOperationStatusObserver(ApplicationMaintenanceService applicationMaintenanceService) {
        this.applicationMaintenanceService = applicationMaintenanceService;
    }

    @Override
    public void handleStatusChange(boolean newStatus) {
        if (newStatus) {
            applicationMaintenanceService.startMaintenanceMode();
        } else {
            applicationMaintenanceService.stopMaintenanceMode();
        }
    }

    @Override
    public void handleProgressChange(float newProgress, String newMessage) {

    }
}
