package org.xlrnet.datac.foundation.domain.validation;

import org.springframework.stereotype.Component;
import org.xlrnet.datac.foundation.domain.Sortable;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates that each element in a given collected has a unique sort property.
 */
@Component
public class SortOrderValidator implements ConstraintValidator<Sorted, Collection<? extends Sortable>> {

    public void initialize(Sorted constraint) {
        // No custom initialization necessary
    }

    public boolean isValid(Collection<? extends Sortable> obj, ConstraintValidatorContext context) {
        Set<Integer> sorts = new HashSet<>(obj.size());
        for (Sortable sorted : obj) {
            int sort = sorted.getSort();
            if (sorts.contains(sort)) {
                return false;
            }
            sorts.add(sort);
        }

        return true;
    }

}
