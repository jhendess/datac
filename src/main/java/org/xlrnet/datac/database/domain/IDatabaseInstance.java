package org.xlrnet.datac.database.domain;

public interface IDatabaseInstance {

    /** Returns the name of the instance. */
    String getName();

    /** Returns the type of this instance. */
    InstanceType getInstanceType();
}
