package org.xlrnet.datac.foundation.domain;

/**
 * Current state of the project.
 */
public enum ProjectState {

    /**
     * The project has never been updated.
     */
    NEW,

    /**
     * The project update finished successfully.
     */
    FINISHED,

    /**
     * The project is currently being updated.
     */
    UPDATING,

    /**
     * No changelog file was found.
     */
    MISSING_LOG,

    /**
     * The last update failed.
     */
    ERROR,
}
