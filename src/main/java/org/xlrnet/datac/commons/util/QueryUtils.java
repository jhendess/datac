package org.xlrnet.datac.commons.util;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;

import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.shared.data.sort.SortDirection;

/**
 * Utility methods for performing query related stuff.
 */
public final class QueryUtils {

    private QueryUtils() {
        // No instances allowed
    }

    public static Sort convertVaadinToSpringSort(List<QuerySortOrder> sortOrderList) {
        List<Sort.Order> springOrders = sortOrderList.stream().map(sortOrder -> new Sort.Order(vaadinToSpringDirection(sortOrder.getDirection()), sortOrder.getSorted())).collect(Collectors.toList());
        return new Sort(springOrders);
    }

    private static Sort.Direction vaadinToSpringDirection(SortDirection direction) {
        Sort.Direction springDirection;
        switch (direction) {
            case ASCENDING:
                springDirection = Sort.Direction.ASC;
                break;
            case DESCENDING:
                springDirection = Sort.Direction.DESC;
                break;
            default:
                throw new IllegalStateException("Illegal direction: " + direction);
        }
        return springDirection;
    }
}
