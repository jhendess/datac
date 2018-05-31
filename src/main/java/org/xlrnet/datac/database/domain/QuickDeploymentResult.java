package org.xlrnet.datac.database.domain;

import org.xlrnet.datac.database.util.DeploymentPhase;

import lombok.Value;

@Value
public class QuickDeploymentResult {

    /** The phase in which the deployment failed. */
    private final DeploymentPhase phase;

    /** Flag whether the deployment was successful or not. */
    private final boolean successful;

    /** Error message if the deployment wasn't successful. */
    private final String errorMessage;

    public static QuickDeploymentResult success() {
        return new QuickDeploymentResult(null, true, null);
    }

    public static QuickDeploymentResult failed(DeploymentPhase phase, String errorMessage) {
        return new QuickDeploymentResult(phase, false, errorMessage);
    }
}
