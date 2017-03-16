package org.xlrnet.datac.commons.exception;

/**
 * Exception which indicates that releasing a lock failed because there was no such lock present.
 */
public class NoSuchLockException extends DatacRuntimeException {

    public NoSuchLockException() {
    }

    public NoSuchLockException(String message) {
        super(message);
    }

    public NoSuchLockException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchLockException(Throwable cause) {
        super(cause);
    }
}
