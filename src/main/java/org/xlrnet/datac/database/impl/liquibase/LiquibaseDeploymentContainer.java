package org.xlrnet.datac.database.impl.liquibase;

import java.util.Collections;
import java.util.List;

import org.xlrnet.datac.database.api.IPreparedDeploymentContainer;
import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.foundation.domain.Project;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class LiquibaseDeploymentContainer implements IPreparedDeploymentContainer {

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private Project project;

    @Setter(AccessLevel.PROTECTED)
    private List<String> generatedSql;

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private DeploymentInstance targetInstance;

    @Override
    public List<String> getGeneratedSql() {
        return Collections.unmodifiableList(generatedSql);
    }
}
