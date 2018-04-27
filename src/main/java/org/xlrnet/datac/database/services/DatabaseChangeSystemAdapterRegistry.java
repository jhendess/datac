package org.xlrnet.datac.database.services;

import static org.xlrnet.datac.commons.util.Preconditions.checkConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.commons.lifecycle.AbstractLifecycleComponent;
import org.xlrnet.datac.database.api.DatabaseChangeSystemAdapter;
import org.xlrnet.datac.database.api.DatabaseChangeSystemMetaInfo;
import org.xlrnet.datac.foundation.configuration.StartupPhases;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.api.VcsMetaInfo;

import com.google.common.collect.ImmutableList;

import lombok.extern.slf4j.Slf4j;

/**
 * Service which provides central access to the available change indexing adapters. The adapters will be registered on application
 * startup.
 */
@Slf4j
@Service
@Scope("singleton")
public class DatabaseChangeSystemAdapterRegistry extends AbstractLifecycleComponent {

     /* All raw discovered vcs adapters. */
    private final List<DatabaseChangeSystemAdapter> dcsAdapters;

    /**
     * List of all registered VCS metadata.
     */
    private List<DatabaseChangeSystemMetaInfo> dcsAdapterMetaInfo;

    /**
     * Map for resolving meta infos to the concrete adapter.
     */
    private Map<DatabaseChangeSystemMetaInfo, DatabaseChangeSystemAdapter> metaInfoAdapterMap = new HashMap<>();

    @Autowired
    public DatabaseChangeSystemAdapterRegistry(List<DatabaseChangeSystemAdapter> dcsAdapters) {
        super();
        this.dcsAdapters = dcsAdapters;
    }

    @Override
    public int getPhase() {
        return StartupPhases.CONFIGURATION;
    }

    /**
     * Returns the available {@link VcsMetaInfo} for all currently registered VCS adapters.
     *
     * @return available metadata for all currently registered VCS adapters.
     */
    public ImmutableList<DatabaseChangeSystemMetaInfo> listSupportedDatabaseChangeSystems() {
        return ImmutableList.copyOf(dcsAdapterMetaInfo);
    }

    @Override
    protected void onStart() {
        dcsAdapterMetaInfo = new ArrayList<>(dcsAdapters.size());
        for (DatabaseChangeSystemAdapter dcsAdapter : dcsAdapters) {
            try {
                registerDcsAdapter(dcsAdapter);
            } catch (RuntimeException e) {
                LOGGER.error("Registration of database change system adapter {} failed", dcsAdapter.getClass().getName(), e);
                throw e;
            }
        }
    }

    private void registerDcsAdapter(DatabaseChangeSystemAdapter dcsAdapter) {
        DatabaseChangeSystemMetaInfo metaInfo = dcsAdapter.getMetaInfo();

        checkConfiguration(metaInfo != null, "Metainfo for database change adapter %s may not be null", dcsAdapter.getClass().getName());
        checkConfiguration(metaInfo.getAdapterName() != null, "Returned metainfo for database change adapter %s may not be null", dcsAdapter.getClass().getName());

        dcsAdapterMetaInfo.add(metaInfo);
        metaInfoAdapterMap.put(metaInfo, dcsAdapter);

        LOGGER.info("Successfully registered database change system adapter {} ({})", metaInfo.getAdapterName(), dcsAdapter.getClass().getName());
    }

    /**
     * Returns an {@link Optional} of the configured {@link DatabaseChangeSystemAdapter} for a given project.
     * @param project The project for which a adapter should be returned.
     * @return An {@link Optional} of the configured {@link DatabaseChangeSystemAdapter} for a given project.
     */
    @NotNull
    public Optional<DatabaseChangeSystemAdapter> getAdapterByProject(@NotNull Project project) {
        for (DatabaseChangeSystemAdapter dcsAdapter : dcsAdapters) {
            if (StringUtils.equals(project.getChangeSystemAdapterClass(), dcsAdapter.getClass().getName())) {
                return Optional.of(dcsAdapter);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns an {@link Optional} with the adapter that matches a given meta info. If no adapter could be found, the
     * optional will be empty.
     *
     * @param dcsMetaInfo
     *         The meta info to use for searching.
     * @return An {@link Optional} containing the requested {@link DatabaseChangeSystemAdapter} if it is registered.
     */
    @NotNull
    public Optional<DatabaseChangeSystemAdapter> findAdapterByMetaInfo(DatabaseChangeSystemMetaInfo dcsMetaInfo) {
        return Optional.ofNullable(metaInfoAdapterMap.get(dcsMetaInfo));
    }

    @NotNull
    public Optional<DatabaseChangeSystemMetaInfo> findMetaInfoByAdapterClassName(@NotNull String adapterClass) {
        DatabaseChangeSystemMetaInfo metaInfo = null;
        for (Map.Entry<DatabaseChangeSystemMetaInfo, DatabaseChangeSystemAdapter> entry : metaInfoAdapterMap.entrySet()) {
            if (StringUtils.equalsIgnoreCase(entry.getValue().getClass().getName(), adapterClass)) {
                metaInfo = entry.getKey();
            }
        }
        return Optional.ofNullable(metaInfo);
    }
}
