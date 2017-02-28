package org.xlrnet.datac.foundation.domain;

import javax.persistence.*;

/**
 * Abstract class for all entities stored in a database. All entities must inherit from this class to make sure that an
 * id field exists.
 */
@MappedSuperclass
public abstract class AbstractEntity {

    /**
     * Internal id of the user.
     */
    @Id
    @GeneratedValue
    protected Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
