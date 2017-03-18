package org.xlrnet.datac.commons.exception;

/**
 * Technical exception that will be thrown if a project repository is already initialized while trying to initialize it.
 */
public class ProjectAlreadyInitializedException extends DatacTechnicalException {

    public ProjectAlreadyInitializedException(String message) {
        super(message);
    }

    public ProjectAlreadyInitializedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectAlreadyInitializedException(Throwable cause) {
        super(cause);
    }
}
