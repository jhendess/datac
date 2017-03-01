package org.xlrnet.datac.vcs.api;

/**
 * Connection statuses for remote version control systems.
 */
public enum VcsConnectionStatus {

    /** An internal error (e.g. an exception) occurred which prevented the connection check from completing. */
    INTERNAL_ERROR,

    /** No connection could be established. */
    COMMUNICATION_FAILURE,

    /** A connection could be established but the authentication failed. */
    AUTHENTICATION_FAILURE,

    /** A connection could be established but the remote repository couldn't be found. */
    NOT_FOUND,

    /** Connection could be established and credentials are valid. */
    ESTABLISHED;
}
