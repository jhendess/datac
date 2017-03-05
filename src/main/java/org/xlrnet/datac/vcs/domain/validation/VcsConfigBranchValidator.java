package org.xlrnet.datac.vcs.domain.validation;

import org.xlrnet.datac.vcs.domain.Branch;
import org.xlrnet.datac.vcs.domain.VcsConfig;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Class level bean validator which checks if the development branch of a {@link VcsConfig} is also marked as a tracked
 * branch.
 */
public class VcsConfigBranchValidator implements ConstraintValidator<ValidReleaseBranches, VcsConfig> {

    @Override
    public void initialize(ValidReleaseBranches constraint) {
        // No custom initialization necessary
    }

    @Override
    public boolean isValid(VcsConfig obj, ConstraintValidatorContext context) {
        Branch developmentBranch = obj.getDevelopmentBranch();
        return (obj.getBranches().contains(developmentBranch) && developmentBranch.isWatched());
    }
}
