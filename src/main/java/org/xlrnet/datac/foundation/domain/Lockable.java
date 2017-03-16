package org.xlrnet.datac.foundation.domain;

import org.jetbrains.annotations.NotNull;

/**
 * An object which can be locked in a central service.
 */
public interface Lockable {

    /**
     * A custom key that will be used in the central locking service for locking this object. The locking key for two
     * different objects should be the same if they refer to the same logical entity (e.g. two different states of
     * entities in the database).
     *
     * @return A key that can be used for locking.
     */
    @NotNull
    String getLockKey();

}
