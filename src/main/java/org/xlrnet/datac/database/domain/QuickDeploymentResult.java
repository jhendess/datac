package org.xlrnet.datac.database.domain;

import java.util.ArrayList;
import java.util.List;

import org.xlrnet.datac.database.util.DeploymentPhase;

import lombok.Value;

/**
 * Result status object of a quick deployment.
 */
@Value
public class QuickDeploymentResult {

    /** The phase in which the deployment failed. */
    private final DeploymentPhase phase;

    /** Flag whether the deployment was successful or not. */
    private final boolean successful;

    /** Error message if the deployment wasn't successful. */
    private final String errorMessage;

    /** List of all executed instance deployments. */
    private final List<InstanceDeploymentResult> instanceDeploymentResultList;

    public static QuickDeploymentResult success(List<InstanceDeploymentResult> instanceDeploymentResults) {
        return new QuickDeploymentResult(null, true, null, instanceDeploymentResults);
    }

    public static QuickDeploymentResult failed(DeploymentPhase phase, String errorMessage, List<InstanceDeploymentResult> instanceDeploymentResults) {
        return new QuickDeploymentResult(phase, false, errorMessage, instanceDeploymentResults);
    }

    public static QuickDeploymentResult failed(DeploymentPhase phase, String errorMessage) {
        return failed(phase, errorMessage, new ArrayList<>());
    }
}
