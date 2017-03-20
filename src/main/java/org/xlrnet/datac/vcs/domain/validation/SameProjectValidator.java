package org.xlrnet.datac.vcs.domain.validation;

import java.util.Objects;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.xlrnet.datac.vcs.domain.Revision;

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
         if (!Objects.equals(parent.getProject(), obj.getProject())) {
            valid = false;
         }
      }
      return valid;
   }
}
