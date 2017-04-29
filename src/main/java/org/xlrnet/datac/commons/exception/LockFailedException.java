package org.xlrnet.datac.commons.exception;

import org.xlrnet.datac.foundation.domain.Lockable;

/**
 * Exception which indicates that acquiring a lock failed.
 */
public class LockFailedException extends DatacTechnicalException {

    public LockFailedException(Lockable lockable) {
        super("Locking object " + (lockable != null ? lockable.getLockKey() : "null") + " failed");
    }
}
