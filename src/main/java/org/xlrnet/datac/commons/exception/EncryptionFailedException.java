package org.xlrnet.datac.commons.exception;

import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

/**
 * Exception which indicates a critical error while trying to encrypting data.
 */
public class EncryptionFailedException extends DatacRuntimeException {

    public EncryptionFailedException(EncryptionOperationNotPossibleException e) {
        super(e);
    }
}
