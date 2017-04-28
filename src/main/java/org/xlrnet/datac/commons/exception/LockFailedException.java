package org.xlrnet.datac.commons.exception;

import org.xlrnet.datac.foundation.domain.Lockable;

/**
 * Exception which indicates that acquiring a lock failed.
 */
public class LockFailedException extends DatacTechnicalException {

    private final Lockable lockable;

    public LockFailedException(Lockable lockable, Lockable lockable1) {
        super("Locking object " + (lockable != null ? lockable.getLockKey() : "null") + " failed");
        this.lockable = lockable1;
    }

    public Lockable getLockable() {
        return lockable;
    }
}
