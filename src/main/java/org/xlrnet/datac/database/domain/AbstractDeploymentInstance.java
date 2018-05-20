package org.xlrnet.datac.database.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;
import org.xlrnet.datac.foundation.domain.AbstractEntity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

@MappedSuperclass
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractDeploymentInstance extends AbstractEntity implements IDatabaseInstance {

    /** Name of the deployment instance. */
    @Setter
    @Getter
    @NotEmpty
    @Size(max = 50)
    @Column(name = "name")
    private String name;

}
