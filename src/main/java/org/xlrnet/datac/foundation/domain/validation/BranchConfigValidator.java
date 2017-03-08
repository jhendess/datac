package org.xlrnet.datac.foundation.domain.validation;

import org.xlrnet.datac.foundation.domain.Project;
import org.xlrnet.datac.vcs.domain.Branch;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Class level bean validator which checks if a {@link Project} contains exactly one development branch
 */
public class BranchConfigValidator implements ConstraintValidator<ValidBranches, Project> {

    @Override
    public void initialize(ValidBranches constraint) {
        // No custom initialization necessary
    }

    @Override
    public boolean isValid(Project obj, ConstraintValidatorContext context) {
        int devBranches = 0;
        for (Branch branch : obj.getBranches()) {
            if (branch.isDevelopment()) {
                devBranches++;
            }
        }

        return devBranches == 1;
    }
}
