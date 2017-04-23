package org.xlrnet.datac.commons.util;

import org.xlrnet.datac.foundation.domain.Sortable;

/**
 * Comparator for sorting instances of {@link Sortable}.
 */
public class SortableComparator implements java.util.Comparator<Sortable> {

    @Override
    public int compare(Sortable o1, Sortable o2) {
        return o1.getSort() > o2.getSort() ? +1 : o1.getSort() < o2.getSort() ? -1 : 0;
    }
}
