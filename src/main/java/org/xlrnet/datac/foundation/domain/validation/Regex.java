package org.xlrnet.datac.foundation.domain.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation constraint which verifies if the annotated object can be compiled using {@link
 * java.util.regex.Pattern#compile(String)}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@Constraint(validatedBy = RegexValidator.class)
public @interface Regex {

    String message() default "No valid regular expression.";

    Class<? extends Payload>[] payload() default {};

    Class<?>[] groups() default {};

}
