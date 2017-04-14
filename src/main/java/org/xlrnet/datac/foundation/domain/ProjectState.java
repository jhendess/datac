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
     * The last update failed.
     */
    ERROR,
}
