package org.xlrnet.datac.vcs.api;

import org.xlrnet.datac.commons.exception.DatacTechnicalException;

/**
 * Technical exception which can be thrown when the connection to a remote VCS repository fails.
 */
public class VcsConnectionException extends DatacTechnicalException {

    public VcsConnectionException(String message) {
        super(message);
    }

    public VcsConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public VcsConnectionException(Throwable cause) {
        super(cause);
    }
}
