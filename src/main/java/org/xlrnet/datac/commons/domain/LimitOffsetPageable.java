package org.xlrnet.datac.commons.domain;

import static com.google.common.base.Preconditions.checkArgument;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * A custom implementation of {@link org.springframework.data.domain.Pageable} which supports limit and offset.
 */
public class LimitOffsetPageable implements Pageable {

    private final Sort sort;
    private int limit = 0;
    private int offset = 0;

    public LimitOffsetPageable(int limit, int offset) {
        this(limit, offset, null);
    }

    public LimitOffsetPageable(int limit, int offset, Sort sort) {
        checkArgument(limit > 0, "Limit must be greater than zero");
        checkArgument(offset >= 0, "Offset must not be less than zero");

        this.limit = limit;
        this.offset = offset;
        this.sort = sort;
    }

    @Override
    public int getPageNumber() {
        return 0;
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return null;
    }

    @Override
    public Pageable previousOrFirst() {
        return this;
    }

    @Override
    public Pageable first() {
        return this;
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }
}
