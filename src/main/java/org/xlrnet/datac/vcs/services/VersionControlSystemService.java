package org.xlrnet.datac.vcs.services;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.vcs.api.VcsAdapter;
import org.xlrnet.datac.vcs.api.VcsMetaInfo;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

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

     * @return available metadata for all currently registered VCS adapters.
     */
    public ImmutableList<VcsMetaInfo> listSupportedVersionControlSystems() {
        return ImmutableList.copyOf(vcsAdapterMetaInfo);
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

    private void registerVcsAdapter(VcsAdapter vcsAdapter) {
        VcsMetaInfo metaInfo = vcsAdapter.getMetaInfo();
        vcsAdapterMetaInfo.add(metaInfo);
        // TODO: Do some registration stuff
        LOGGER.info("Successfully registered VCS adapter {} ({}) for system {}", metaInfo.getAdapterName(), vcsAdapter.getClass().getName(), metaInfo.getVcsName());
    }

}
