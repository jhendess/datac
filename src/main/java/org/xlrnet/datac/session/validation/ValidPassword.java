package org.xlrnet.datac.session.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Validation constraint for matching the default password rules.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Constraint(validatedBy = PasswordValidator.class)
public @interface ValidPassword {

    String message() default "The given password doesn't meet the requirements.";

    Class<? extends Payload>[] payload() default {};

    Class<?>[] groups() default {};
}
