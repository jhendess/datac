package org.xlrnet.datac.foundation.domain.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for validating if a {@link org.xlrnet.datac.foundation.domain.Project} contains exactly one valid
 * development branch.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BranchConfigValidator.class)
public @interface ValidBranches {

    String message() default "Project must have exactly one development branch.";

    Class<? extends Payload>[] payload() default {};

    Class<?>[] groups() default {};

}
