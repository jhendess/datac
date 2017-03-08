package org.xlrnet.datac.foundation.domain.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Validates if something is a valid regular expression.
 */
public class RegexValidator implements ConstraintValidator<Regex, String> {

    public void initialize(Regex constraint) {
        // No initialization necessary
    }

    public boolean isValid(String obj, ConstraintValidatorContext context) {
        try {
            Pattern.compile(obj);
            return true;
        } catch (PatternSyntaxException p) {    // NOSONAR: No logging of exception necessary
            return false;
        }
    }

}
