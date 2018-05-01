package org.xlrnet.datac.database.domain;

public interface IDatabaseInstance {

    /** Returns the name of the instance. */
    String getName();

    /** Flag to indicate if the concrete implementation is a group. */
    boolean isGroup();
}
