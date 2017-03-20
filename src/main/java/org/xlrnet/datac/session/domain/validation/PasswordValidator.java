package org.xlrnet.datac.session.domain.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.session.services.PasswordService;

/**
 * Validator for checking passwords.
 */
@Service
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    /** Static password service. */
    private static PasswordService passwordService = new PasswordService();

    @Override
    public void initialize(@NotNull ValidPassword constraintAnnotation) {
        // No initialization necessary
    }

    @Override
    public boolean isValid(@NotNull String value, @NotNull ConstraintValidatorContext context) {
        return passwordService.isValid(value);
    }
}