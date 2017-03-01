package org.xlrnet.datac.vcs.services;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.vcs.api.VcsAdapter;
import org.xlrnet.datac.vcs.api.VcsMetaInfo;

import javax.annotation.PostConstruct;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Service which provides central access to the available VCS adapters. The adapters will be registered on application
 * startup.
 */
@Component
@Scope("singleton")
public class VersionControlSystemService implements Lifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionControlSystemService.class);

    /** All raw discovered vcs adapters. */
    private final List<VcsAdapter> vcsAdapters;

    /** List of all registered VCS metadata. */
    private List<VcsMetaInfo> vcsAdapterMetaInfo;

    /** Map for resolving meta infos to the concrete adapter. */
    private Map<VcsMetaInfo, VcsAdapter> metaInfoAdapterMap = new HashMap<>();

    private boolean running;

    @Autowired
    public VersionControlSystemService(List<VcsAdapter> vcsAdapters) {
        this.vcsAdapters = vcsAdapters;
    }

    @PostConstruct
    void init() {
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
     * @param vcsMetaInfo The meta info to use for searching.
     * @return An {@link Optional} containing the requested {@link VcsAdapter} if it is registered.
     */
    @NotNull
    public Optional<VcsAdapter> findAdapterByMetaInfo(@NotNull VcsMetaInfo vcsMetaInfo) {
        return Optional.ofNullable(metaInfoAdapterMap.get(vcsMetaInfo));
    }

    @Override
    public void start() {
        // TODO: This is probably not necessary
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

    private void registerVcsAdapter(@NotNull VcsAdapter vcsAdapter) {
        VcsMetaInfo metaInfo = vcsAdapter.getMetaInfo();

        checkNotNull(metaInfo, "Metainfo for VCS adapter may not be null", vcsAdapter.getClass().getName());
        checkNotNull(metaInfo.getVcsName(), "VCS name for VCS adapter may not be null", vcsAdapter.getClass().getName());
        checkNotNull(metaInfo.getAdapterName(), "Returned metainfo for VCS adapter may not be null", vcsAdapter.getClass().getName());

        vcsAdapterMetaInfo.add(metaInfo);
        metaInfoAdapterMap.put(metaInfo, vcsAdapter);


        LOGGER.info("Successfully registered VCS adapter {} ({}) for system {}", metaInfo.getAdapterName(), vcsAdapter.getClass().getName(), metaInfo.getVcsName());
    }

}
