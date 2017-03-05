package org.xlrnet.datac.vcs.domain.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for validating if the development branch of a {@link org.xlrnet.datac.vcs.domain.VcsConfig} is also
 * marked as a tracked branch.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = VcsConfigBranchValidator.class)
public @interface ValidReleaseBranches {

    String message() default "The development branch must be selected for tracking.";

    Class<? extends Payload>[] payload() default {};

    Class<?>[] groups() default {};

}
