package org.xlrnet.datac.vcs.services;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.foundation.domain.Lockable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Central locking service for entities. This service may be used for aquiring application wide database locks.
 */
@Service
@Scope("singleton")
public class LockingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockingService.class);

    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * Tries to acquire the lock for the given object. The lock must be manually released by calling {@link
     * #unlock(Lockable)} after the custom logic has finished.
     *
     * @param lockable
     *         The lockable to lock.
     * @return True if the object could be locked, false if not.
     */
    public boolean tryLock(@NotNull Lockable lockable) {
        String key = getLockKey(lockable);
        Lock lock = internalGetLock(key);
        boolean locked = lock.tryLock();    // NOSONAR: This service is used for centrally managing locks across the application
        if (locked) {
            LOGGER.debug("Successfully acquired lock for object {}", key);
        } else {
            LOGGER.debug("Locking object {} failed", key);
        }
        return locked;
    }

    /**
     * Unlocks the given object.
     *
     * @param lockable
     *         The object to unlock.
     */
    public void unlock(@NotNull Lockable lockable) {
        String key = getLockKey(lockable);
        ReentrantLock reentrantLock = internalGetLock(key);
        if (reentrantLock.isLocked()) {
            reentrantLock.unlock();
            LOGGER.debug("Unlocked object {}", key);
        } else {
            LOGGER.warn("No lock present on object {}", key);
        }
    }

    /**
     * Checks if the given object is currently locked.
     *
     * @param lockable
     *         The object to check.
     * @return True if locked, false otherwise.
     */
    public boolean isLocked(@NotNull Lockable lockable) {
        String key = getLockKey(lockable);
        return internalGetLock(key).isLocked();
    }

    private ReentrantLock internalGetLock(String key) {
        return lockMap.computeIfAbsent(key, (x) -> new ReentrantLock());
    }

    @NotNull
    private String getLockKey(@NotNull Lockable lockable) {
        return lockable.getClass().getName() + "_" + lockable.getLockKey();
    }
}
