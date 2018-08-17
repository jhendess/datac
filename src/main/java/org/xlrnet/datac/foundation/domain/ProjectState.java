package org.xlrnet.datac.foundation.domain;

/**
 * Current state of the project.
 */
public enum ProjectState {

    /**
     * The project has never been updated.
     */
    NEW(false),

    /**
     * The project update finished successfully.
     */
    FINISHED(false),

    /**
     * The project repository is being initialized.
     */
    INITIALIZING(false),

    /**
     * The project is currently being updated.
     */
    UPDATING(true),

    /**
     * Database changes are currently being indexed.
     */
    INDEXING(true),

    /**
     * No changelog file was found.
     */
    MISSING_LOG(false),

    /**
     * The last update failed.
     */
    ERROR(false),

    /**
     * Cleaning the project failed. Work folder must be manually analyzed
     */
    DIRTY(false),

    /**
     * The last project update was interrupted on shutdown.
     */
    INTERRUPTED(false),

    /**
     * The project is currently under maintenance.
     **/
    MAINTENANCE(true);

    private final boolean progressable;

    ProjectState(boolean progressable) {
        this.progressable = progressable;
    }

    /**
     * Flag to check if the given progress state can contain progress information.
     */
    public boolean isProgressable() {
        return progressable;
    }
}
