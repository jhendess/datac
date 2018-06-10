package org.xlrnet.datac.database.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.commons.exception.LockFailedException;
import org.xlrnet.datac.database.api.DatabaseChangeSystemAdapter;
import org.xlrnet.datac.database.api.IPreparedDeploymentContainer;
import org.xlrnet.datac.database.domain.ConnectionPingResult;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.database.domain.InstanceDeploymentResult;
import org.xlrnet.datac.database.domain.QuickDeploymentConfig;
import org.xlrnet.datac.database.domain.QuickDeploymentResult;
import org.xlrnet.datac.database.util.DeploymentPhase;
import org.xlrnet.datac.foundation.components.EventLogProxy;
import org.xlrnet.datac.foundation.domain.EventLogMessage;
import org.xlrnet.datac.foundation.domain.EventType;
import org.xlrnet.datac.foundation.domain.MessageSeverity;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.foundation.services.EventLogService;
import org.xlrnet.datac.foundation.ui.components.EntityChangeHandler;
import org.xlrnet.datac.foundation.ui.components.ProgressChangeHandler;
import org.xlrnet.datac.vcs.services.LockingService;

import lombok.extern.slf4j.Slf4j;

/**
 * Central service to perform any kinds of deployment.
 */
@Slf4j
@Service
public class DeploymentManagerService {

    /** The factor which is indicates 100% finished connection checks. */
    private static final float CONNECTION_CHECK_PROGRESS_FACTOR = 0.1f;

    /** Locking service. */
    private final LockingService lockingService;

    /** Service for writing event logs. */
    private final EventLogService eventLogService;

    /** Event logging proxy. */
    private final EventLogProxy eventLogProxy;

    /** Service for connecting to databases. */
    private final ConnectionManagerService connectionManagerService;

    /** Registry for accessing DCS. */
    private final DatabaseChangeSystemAdapterRegistry databaseChangeSystemAdapterRegistry;

    @Autowired
    public DeploymentManagerService(LockingService lockingService, EventLogService eventLogService, EventLogProxy eventLogProxy, ConnectionManagerService connectionManagerService, DatabaseChangeSystemAdapterRegistry databaseChangeSystemAdapterRegistry) {
        this.lockingService = lockingService;
        this.eventLogService = eventLogService;
        this.eventLogProxy = eventLogProxy;
        this.connectionManagerService = connectionManagerService;
        this.databaseChangeSystemAdapterRegistry = databaseChangeSystemAdapterRegistry;
    }

    /**
     * Performs an asynchronous quick deployment for a given project on one or more deployment instances.
     * @param project The project for which the deployment should be performed. This configuration data is used to resolve information about required adapters.
     * @param deploymentConfig  The deployment config.
     * @param changeSet        The change set to execute.
     * @param changeHandler    The handler for progress updates.
     */
    @Async
    public void startAsynchronousQuickDeployment(@NotNull Project project, @NotNull QuickDeploymentConfig deploymentConfig, @NotNull DatabaseChangeSet changeSet, @NotNull ProgressChangeHandler changeHandler, EntityChangeHandler<QuickDeploymentResult> finishedHandler) {
        QuickDeploymentResult result;
        try {
            result = performQuickDeployment(project, deploymentConfig, changeSet, changeHandler);
        } catch (LockFailedException e) {       // NOSONAR: No logging of exception necessary
                LOGGER.warn("Update of project {} [id={}] failed because project is locked", project.getName(), project.getId());
            result = QuickDeploymentResult.failed(DeploymentPhase.LOCKING, "Project is locked");
        }
        finishedHandler.onChange(result);
    }

    private QuickDeploymentResult performQuickDeployment(@NotNull Project project, @NotNull QuickDeploymentConfig quickDeploymentConfig, @NotNull DatabaseChangeSet changeSet, @NotNull ProgressChangeHandler changeHandler) throws LockFailedException {
        LOGGER.info("Starting quick deployment for project {} and change set {} on instances {}", project.getName(), changeSet.getInternalId(), quickDeploymentConfig);
        eventLogProxy.setDelegate(eventLogService.newEventLog().setType(EventType.QUICK_DEPLOYMENT).setProject(project));
        List<InstanceDeploymentResult> instanceDeploymentResults = new ArrayList<>();
        try {
            if (lockingService.tryLock(project)) {
                // Open DCS adapter
                Optional<DatabaseChangeSystemAdapter> databaseChangeSystemAdapterOptional = databaseChangeSystemAdapterRegistry.getAdapterByProject(project);
                if (!databaseChangeSystemAdapterOptional.isPresent()) {
                    handleDeploymentFailure(DeploymentPhase.CONFIG_VALIDATION);
                    return QuickDeploymentResult.failed(DeploymentPhase.CONFIG_VALIDATION, "Unable to open DCS adapter");
                }
                DatabaseChangeSystemAdapter dcsAdapter = databaseChangeSystemAdapterOptional.get();
                Set<DeploymentInstance> instances = quickDeploymentConfig.getInstances();

                changeHandler.handleProgressChange(0, "Validating deployment configuration ...");
                // Pre-validate instance configuration
                if (!validateInstances(project, instances)) {
                    handleDeploymentFailure(DeploymentPhase.CONFIG_VALIDATION);
                    return QuickDeploymentResult.failed(DeploymentPhase.CONFIG_VALIDATION, "At least one instance is not configured correctly.");
                }
                // Check connection to instances
                if (!validateInstanceConnections(instances, changeHandler)) {
                    handleDeploymentFailure(DeploymentPhase.CONNECTION_VALIDATION);
                    return QuickDeploymentResult.failed(DeploymentPhase.CONNECTION_VALIDATION, "Connection failure.");
                }

                // Perform deployment
                int i = 0;
                for (DeploymentInstance targetInstance : instances) {
                    changeHandler.handleProgressChange((float) i++ / instances.size(), String.format("Deploying %s...", targetInstance.getFullPath()));
                    IPreparedDeploymentContainer preparedDeployment = dcsAdapter.prepareDeployment(project, targetInstance, changeSet);
                }

                LOGGER.info("Finished quick deployment for project {} and change set {} on instances {}", project.getName(), changeSet.getInternalId(), quickDeploymentConfig);
                changeHandler.handleProgressChange(1, "Finished quick deployment");
            } else {
                throw new LockFailedException(project);
            }
        } catch (RuntimeException | DatacTechnicalException e) {
            LOGGER.error("Unexpected error during quick deployment for project {} [id={}]", project.getName(), project.getId(), e);
            eventLogService.addExceptionToEventLog(eventLogProxy, "Unexpected error during quick deployment",  e);
        } finally {
            lockingService.unlock(project);
            eventLogService.save(eventLogProxy);
        }
        return QuickDeploymentResult.success(instanceDeploymentResults);
    }

    private void handleDeploymentFailure(DeploymentPhase deploymentPhase) {
        eventLogProxy.addMessage(new EventLogMessage("Deployment failure in phase " + deploymentPhase).setSeverity(MessageSeverity.ERROR));
        LOGGER.error("Deployment failure in phase {}", deploymentPhase);
    }

    private boolean validateInstances(Project project, Collection<DeploymentInstance> targetInstances) {
        boolean valid = true;
        for (DeploymentInstance targetInstance : targetInstances) {
            if (!Objects.equals(project, targetInstance.getGroup().getProject())) {
                String message = String.format("Target instance %s is not part of project %s", targetInstance.getFullPath(), project.getName());
                LOGGER.warn(message);
                eventLogProxy.addMessage(new EventLogMessage(message));
                valid = false;
            }
        }
        return valid;
    }

    private boolean validateInstanceConnections(Collection<DeploymentInstance> targetInstances, ProgressChangeHandler progressChangeHandler) {
        boolean valid = true;
        int totalInstances = targetInstances.size();
        int currentInstance = 0;
        for (DeploymentInstance targetInstance : targetInstances) {
            progressChangeHandler.handleProgressChange(currentInstance / totalInstances * CONNECTION_CHECK_PROGRESS_FACTOR, String.format("Checking connection to %s ...", targetInstance.getConnection().getName()));
            ConnectionPingResult connectionPingResult = connectionManagerService.pingConnection(targetInstance.getConnection());
            if (!connectionPingResult.isConnected()) {
                eventLogService.addExceptionToEventLog(eventLogProxy, "Connecting to database instance %s using connection %s failed", connectionPingResult.getException());
                valid = false;
                break;
            }
        }
        return valid;
    }

}
