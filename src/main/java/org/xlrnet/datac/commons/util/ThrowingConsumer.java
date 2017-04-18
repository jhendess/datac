package org.xlrnet.datac.commons.util;

import org.xlrnet.datac.commons.exception.DatacTechnicalException;

/**
 * Represents an operation that accepts a single input argument and returns no result. Unlike most other functional
 * interfaces, {@link ThrowingConsumer} is expected to operate via side-effects. This is a functional interface whose
 * functional method is {@link #accept(Object)}. The method may throw a technical exception.
 */
@FunctionalInterface
public interface ThrowingConsumer<T> {

    void accept(T var1) throws DatacTechnicalException;
}
