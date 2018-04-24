package org.xlrnet.datac.foundation.domain;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.Data;

/**
 * Abstract class for all entities stored in a database. All entities must inherit from this class to make sure that an
 * id field exists.
 */
@Data
@MappedSuperclass
public abstract class AbstractEntity {

    /**
     * Internal id of the user.
     */
    @Id
    @GeneratedValue
    protected Long id;

    public boolean isPersisted() {
        return getId() != null;
    }
}
