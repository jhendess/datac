package org.xlrnet.datac.vcs.services;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.foundation.configuration.StartupPhases;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.api.VcsAdapter;
import org.xlrnet.datac.vcs.api.VcsMetaInfo;

import com.google.common.collect.ImmutableList;

/**
 * Service which provides central access to the available VCS adapters. The adapters will be registered on application
 * startup.
 */
@Component
@Scope("singleton")
public class VersionControlSystemRegistry implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionControlSystemRegistry.class);

    /**
     * All raw discovered vcs adapters.
     */
    private final List<VcsAdapter> vcsAdapters;

    /**
     * List of all registered VCS metadata.
     */
    private List<VcsMetaInfo> vcsAdapterMetaInfo;

    /**
     * Map for resolving meta infos to the concrete adapter.
     */
    private Map<VcsMetaInfo, VcsAdapter> metaInfoAdapterMap = new HashMap<>();

    /**
     * Flag if the service is running.
     */
    private boolean running;

    @Autowired
    public VersionControlSystemRegistry(List<VcsAdapter> vcsAdapters) {
        this.vcsAdapters = vcsAdapters;
    }

    /**
     * Tries to find the specific metadata for a given adapter class name.
     *
     * @param adapterClass
     *         The class of the adapter to whom the metadata belongs.
     * @return An optional containing the {@link VcsMetaInfo} object or empty.
     */
    @NotNull
    public Optional<VcsMetaInfo> findMetaInfoByAdapterClassName(@NotNull String adapterClass) {
        VcsMetaInfo metaInfo = null;
        for (Map.Entry<VcsMetaInfo, VcsAdapter> entry : metaInfoAdapterMap.entrySet()) {
            if (StringUtils.equalsIgnoreCase(entry.getValue().getClass().getName(), adapterClass)) {
                metaInfo = entry.getKey();
            }
        }
        return Optional.ofNullable(metaInfo);
    }

    /**
     * Tries to find the first available metadata of an adapter supporting the given VCS type. Works case insensitive.
     *
     * @param type
     *         The type of VCS to look for. This is the value returned by {@link VcsMetaInfo#getVcsName()}.
     * @return An optional containing the {@link VcsMetaInfo} or empty.
     */
    @NotNull
    public Optional<VcsMetaInfo> findMetaInfoByVcsType(@NotNull String type) {
        VcsMetaInfo metaInfo = null;
        for (VcsMetaInfo m : metaInfoAdapterMap.keySet()) {
            if (StringUtils.equalsIgnoreCase(m.getVcsName(), type)) {
                metaInfo = m;
            }
        }

        return Optional.ofNullable(metaInfo);
    }

    /**
     * Try to resolve the correct VCS adapter for a given project. If no adapter with the same class could be found, the
     * application tries to fall back to a adapter which implements the same VCS type.
     *
     * @return The correct VCS adapter for the project.
     * @throws DatacTechnicalException
     *         Will be thrown if no VCS adapter could be resolved
     */
    @NotNull
    public VcsAdapter getVcsAdapter(@NotNull Project project) throws DatacTechnicalException {
        Optional<VcsMetaInfo> metaInfo = findMetaInfoByAdapterClassName(project.getVcsAdapterClass());
        if (!metaInfo.isPresent()) {
            metaInfo = findMetaInfoByVcsType(project.getVcsType());
            metaInfo.ifPresent(m -> LOGGER.warn("No VCS of class {} found - falling back to adapter {} with same type {}", project.getVcsAdapterClass(), m.getAdapterName(), m.getVcsName()));
        }
        if (!metaInfo.isPresent()) {
            throw new DatacTechnicalException("No VCS adapter of type " + project.getVcsType() + " or class " + project.getVcsAdapterClass() + " is available");
        }

        Optional<VcsAdapter> adapterByMetaInfo = findAdapterByMetaInfo(metaInfo.get());
        if (adapterByMetaInfo.isPresent()) {
            return adapterByMetaInfo.get();
        } else {
            throw new DatacTechnicalException("Resolving VCS adapter failed");
        }
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public int getPhase() {
        return StartupPhases.CONFIGURATION;
    }

    @Override
    public void start() {
        if (!running) {
            init();
        }
    }

    @Override
    public void stop() {
        LOGGER.info("Shutting down VCS adapters");
        // TODO: Shut down the adapters
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void init() {
        vcsAdapterMetaInfo = new ArrayList<>(vcsAdapters.size());
        for (VcsAdapter vcsAdapter : vcsAdapters) {
            try {
                registerVcsAdapter(vcsAdapter);
            } catch (RuntimeException e) {
                LOGGER.error("Registration of VCS adapter {} failed", vcsAdapter.getClass().getName(), e);
                throw e;
            }
        }
        running = true;
    }

    /**
     * Returns the available {@link VcsMetaInfo} for all currently registered VCS adapters.
     *
     * @return available metadata for all currently registered VCS adapters.
     */
    public ImmutableList<VcsMetaInfo> listSupportedVersionControlSystems() {
        return ImmutableList.copyOf(vcsAdapterMetaInfo);
    }

    /**
     * Returns an {@link Optional} with the adapter that matches a given meta info. If no adapter could be found, the
     * optional will be empty.
     *
     * @param vcsMetaInfo
     *         The meta info to use for searching.
     * @return An {@link Optional} containing the requested {@link VcsAdapter} if it is registered.
     */
    @NotNull
    public Optional<VcsAdapter> findAdapterByMetaInfo(@NotNull VcsMetaInfo vcsMetaInfo) {
        return Optional.ofNullable(metaInfoAdapterMap.get(vcsMetaInfo));
    }

    private void registerVcsAdapter(@NotNull VcsAdapter vcsAdapter) {
        VcsMetaInfo metaInfo = vcsAdapter.getMetaInfo();

        checkNotNull(metaInfo, "Metainfo for VCS adapter %s may not be null", vcsAdapter.getClass().getName());
        checkNotNull(metaInfo.getVcsName(), "VCS name for VCS adapter %s may not be null", vcsAdapter.getClass().getName());
        checkNotNull(metaInfo.getAdapterName(), "Returned metainfo for VCS adapter %s may not be null", vcsAdapter.getClass().getName());

        vcsAdapterMetaInfo.add(metaInfo);
        metaInfoAdapterMap.put(metaInfo, vcsAdapter);


        LOGGER.info("Successfully registered VCS adapter {} ({}) for system {}", metaInfo.getAdapterName(), vcsAdapter.getClass().getName(), metaInfo.getVcsName());
    }

}
