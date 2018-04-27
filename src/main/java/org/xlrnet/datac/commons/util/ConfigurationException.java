package org.xlrnet.datac.commons.util;

/**
 * Exception which may be thrown on configuration errors.
 */
public class ConfigurationException extends TechnicalRuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }
    
    public ConfigurationException(Exception e) {
        super(e);
    }
}
