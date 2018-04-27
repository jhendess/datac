package org.xlrnet.datac.commons.util;

/**
 * The class {@link TechnicalRuntimeException} represents any kind of exception that may be thrown during a datac
 * call. It is not expected that the application handles the exception successfully.
 */
public class TechnicalRuntimeException extends RuntimeException {

    public TechnicalRuntimeException() {
        super();
    }

    public TechnicalRuntimeException(String message) {
        super(message);
    }

    public TechnicalRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public TechnicalRuntimeException(Throwable cause) {
        super(cause);
    }

}
