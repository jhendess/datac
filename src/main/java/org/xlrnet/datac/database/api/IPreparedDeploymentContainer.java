package org.xlrnet.datac.database.api;

import java.util.List;

import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.foundation.domain.Project;

public interface IPreparedDeploymentContainer {

    Project getProject();

    DeploymentInstance getTargetInstance();

    List getGeneratedSql();
}
