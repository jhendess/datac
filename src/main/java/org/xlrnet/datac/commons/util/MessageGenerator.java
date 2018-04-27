package org.xlrnet.datac.commons.util;

import org.jetbrains.annotations.NotNull;

/**
 * Functional interface which can be used to provide a String message based on a given entity.
 * @param <T>
 */
@FunctionalInterface
public interface MessageGenerator<T> {

    /**
     * Generate a message for the given entity.
     * @param entity The entity for which a message should be generated. Entity is never null.
     * @return The generated String message.
     */
    @NotNull
    String generate(@NotNull T entity);
}
