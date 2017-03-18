package org.xlrnet.datac.commons.exception;

/**
 * Exception thrown by a VCS system.
 */
public class VcsRepositoryException extends DatacTechnicalException {

    public VcsRepositoryException(String message) {
        super(message);
    }

    public VcsRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public VcsRepositoryException(Throwable cause) {
        super(cause);
    }
}
