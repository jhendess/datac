package org.xlrnet.datac.database.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;
import org.jetbrains.annotations.Nullable;
import org.xlrnet.datac.foundation.domain.AbstractEntity;
import org.xlrnet.datac.vcs.domain.Branch;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

@MappedSuperclass
@EqualsAndHashCode(callSuper = true, of = "name")
public abstract class AbstractDeploymentInstance extends AbstractEntity implements IDatabaseInstance {

    /** Name of the deployment instance. */
    @Setter
    @Getter
    @NotEmpty
    @Size(max = 50)
    @Column(name = "name")
    private String name;

    @Setter
    @Getter
    @ManyToOne(optional = true)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    /**
     * Returns the actual branch that is used in this deployment instance. If this object doesn't have its branch
     * property set, the value is inherited from the parent.
     *
     * @return the actual branch that is used in this deployment instance.
     */
    @Nullable
    public Branch getActualBranch() {
        Branch actualBranch = branch;
        if (actualBranch == null) {
            AbstractDeploymentInstance parent = getParent();
            if (parent != null) {
                actualBranch = parent.getActualBranch();
            }
        }
        return actualBranch;
    }

    /**
     * Returns the parent group of this instance or null if there is no parent (i.e. this is the root).
     */
    abstract AbstractDeploymentInstance getParent();

}
