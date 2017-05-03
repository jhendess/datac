package org.xlrnet.datac.session.domain.validation;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.xlrnet.datac.session.services.CryptoService;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator for checking passwords.
 */
@Service
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    /**
     * Static password service.
     */
    private static CryptoService cryptoService = new CryptoService();

    @Override
    public void initialize(@NotNull ValidPassword constraintAnnotation) {
        // No initialization necessary
    }

    @Override
    public boolean isValid(@NotNull String value, @NotNull ConstraintValidatorContext context) {
        return cryptoService.isValid(value);
    }
}