package org.xlrnet.datac.database.util;

import com.vaadin.data.ValueProvider;
import org.xlrnet.datac.database.domain.AbstractDeploymentInstance;
import org.xlrnet.datac.database.domain.IDatabaseInstance;
import org.xlrnet.datac.vcs.domain.Branch;

/**
 * Renders the name of the inherited branch associated with {@link IDatabaseInstance}.
 */
public class InheritedBranchNameProvider implements ValueProvider<IDatabaseInstance, String> {

    @Override
    public String apply(IDatabaseInstance v) {
        String value = null;
        if (v instanceof AbstractDeploymentInstance) {
            AbstractDeploymentInstance i = ((AbstractDeploymentInstance) v);
            if (i.getBranch() == null) {
                Branch actualBranch = i.getActualBranch();
                if (actualBranch != null) {
                    value = actualBranch.getName() + " (inherited)";
                }
            } else {
                value = i.getBranch().getName();
            }
        }
        return value;
    }
}
