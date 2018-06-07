package org.xlrnet.datac.database.domain;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration of a single quick deployment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuickDeploymentConfig {

    /** Target instances for deployment. */
    private Set<DeploymentInstance> instances;

    /** Abort all coming deployments if a single deployment fails. */
    private boolean abortOnFailure;
}
