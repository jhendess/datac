package org.xlrnet.datac.session.ui.components.project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.exception.DatacRuntimeException;
import org.xlrnet.datac.commons.exception.DatacTechnicalException;
import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.Revision;

import javax.annotation.PostConstruct;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;

/**
 * Abstract implementation of a single layout on the project sub view. Each layout is usually used as a tab.
 */
public abstract class AbstractProjectLayout extends MVerticalLayout {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProjectLayout.class);

    /** The project to which this layout belongs. */
    private Project project;

    /** The revision which must be displayed. */
    private Revision revision;

    /** The branch to which the revision belongs. May be null if there is no branch. */
    private Branch branch;

    /** Flag if the content has been initialized after a revision change. */
    private boolean contentRefreshed = false;

    @PostConstruct
    private void init() {
        initialize();
    }

    public void setProject(Project project) {
        this.project = project;
    }

    /**
     * Sets the active revision to display and refresh the content. If the revision didn't change, no update will be
     * performed.
     *
     * @param activeRevision
     *         The revision to set active.
     */
    public void setActiveRevisionAndRefreshContent(@NotNull Revision activeRevision, @Nullable Branch activeBranch) {
        checkState(project != null, "Project must be set before a project layout is refreshed");
        if (!Objects.equals(revision, activeRevision) && (activeBranch == null || !Objects.equals(branch, activeBranch))) {
            this.revision = activeRevision;
            this.branch = activeBranch;
            try {
                refreshContent();
                contentRefreshed = true;
            } catch (DatacTechnicalException e) {
                LOGGER.error("Unknown error occurred while trying to refresh tab", e);
                throw new DatacRuntimeException(e);
            }
        } else {
            LOGGER.trace("No content refresh necessary - revision didn't change");
        }
    }

    /**
     * Returns whether the content has already been updated after a revision change.
     */
    public boolean isContentRefreshed() {
        return contentRefreshed;
    }

    @NotNull
    Project getProject() {
        return project;
    }

    @NotNull
    Revision getRevision() {
        return revision;
    }

    @Nullable
    Branch getBranch() {
        return branch;
    }

    /**
     * Perform necessary initializations on the first load.
     */
    abstract void initialize();

    /**
     * Method will be called always before the layout is requested to refresh its content. This may be used to e.g.
     * reset internal component state (e.g. expandable panels).
     */
    public abstract void beforeContentRefresh();

    /**
     * Updates the internal content based on the latest revision data. This enforces <i>always</i> an update of the
     * underlying components. This method will not be called if the revision data didn't change.
     */
    abstract void refreshContent() throws DatacTechnicalException;

    /**
     * Returns the new title for the parent project subview.
     */
    @NotNull
    public abstract String getTitle();

    /**
     * Returns the new subtitle for the parent project subview.
     */
    @NotNull
    public abstract String getSubtitle();
}
