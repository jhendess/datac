package org.xlrnet.datac.vcs.domain.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Validation constraint which ensures that all parents of a {@link org.xlrnet.datac.vcs.domain.Revision} belong to the
 * same project as the child.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Constraint(validatedBy = SameProjectValidator.class)
public @interface SameProjectParent {

    String message() default "All parent revisions must belong to the same project.";

    Class<? extends Payload>[] payload() default {};

    Class<?>[] groups() default {};
}
