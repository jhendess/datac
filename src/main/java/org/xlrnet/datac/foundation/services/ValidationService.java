package org.xlrnet.datac.foundation.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Set;

/**
 * Service which provides abstractions for bean validation.
 */
@Service
public class ValidationService {

    private final Validator validator;

    @Autowired
    public ValidationService(Validator validator) {
        this.validator = validator;
    }

    /**
     * Checks the given object regarding bean constraints and throws a {@link ConstraintViolationException} if
     * constraints were violated.
     *
     * @param <T> Type of the validated object.
     * @param o The object to validate.
     * @param groups The group or list of groups targeted for validation.
     * @throws ConstraintViolationException Will be thrown if a constraint violation occurred.
     */
    public <T> void checkConstraints(T o, Class<?>... groups) throws ConstraintViolationException {
        Set<ConstraintViolation<T>> violations = validator.validate(o, groups);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException("Violated constraints: " + violations.toString(), violations);
        }
    }
}
