package org.xlrnet.datac.database.domain;

import lombok.Value;

/**
 * Result of a single instance deployment.
 */
@Value
public class InstanceDeploymentResult {

    /** The complete generated SQL for the deployment .*/
    String generatedSql;

    /** The database connection which was used to perform the deployment.*/
    DatabaseConnection targetConnection;
}
