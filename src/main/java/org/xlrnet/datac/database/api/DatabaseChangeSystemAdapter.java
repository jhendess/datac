package org.xlrnet.datac.database.api;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.database.domain.DatabaseChangeSet;
import org.xlrnet.datac.database.domain.DeploymentInstance;
import org.xlrnet.datac.foundation.domain.Project;

/**
 * Adapter which provides methods for interacting with changes specific to a database change system. This includes both
 * indexing changes and generation of SQL scripts. Adapters must be implemented stateless as they will be shared among
 * multiple threads.
 */
public interface DatabaseChangeSystemAdapter {

    @NotNull
    DatabaseChangeSystemMetaInfo getMetaInfo();

    /**
     * Returns all database change sets in the given project. The project will be scanned in its current state.
     * Therefore you have to make sure, that you checked out the correct revision manually before.
     *
     * @param project
     *         The project to index.
     * @return List of database change sets. Begins with the oldest currently present.
     */
    @NotNull
    List<DatabaseChangeSet> listDatabaseChangeSetsForProject(@NotNull Project project) throws DatacTechnicalException;

    /**
     * Prepare a new deployment. The adaptor may decide on its own if a connection to a database is must be established.
     * Implementors may assume that this method is called on a locked project, therefore full
     * access to the underlying VCS is granted.
     * @param project
     * @param targetInstance
     * @param changeSet
     * @return
     * @throws DatacTechnicalException
     */
    @NotNull
    IPreparedDeploymentContainer prepareDeployment(@NotNull Project project, @NotNull DeploymentInstance targetInstance, @NotNull DatabaseChangeSet changeSet) throws DatacTechnicalException;
}
