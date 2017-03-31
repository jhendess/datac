package org.xlrnet.datac.vcs.domain.validation;

import org.xlrnet.datac.vcs.domain.Revision;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Objects;

/**
 * Validate that all parents of a {@link org.xlrnet.datac.vcs.domain.Revision} belong to the same project as the child.
 */
public class SameProjectValidator implements ConstraintValidator<SameProjectParent, Revision> {

    public void initialize(SameProjectParent constraint) {
        // No initialization necessary
    }

    public boolean isValid(Revision obj, ConstraintValidatorContext context) {
        boolean valid = true;
        for (Revision parent : obj.getParents()) {
            if (!Objects.equals(parent.getProject().getId(), obj.getProject().getId())) {
                valid = false;
            }
        }
        return valid;
    }
}
