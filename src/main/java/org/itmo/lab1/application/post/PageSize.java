package org.itmo.lab1.application.post;

import org.itmo.lab1.domain.DomainValidationException;

public record PageSize(int value) {

    public static final int DEFAULT = 20;
    public static final int MAX = 50;

    public PageSize {
        if (value < 1 || value > MAX) {
            throw new DomainValidationException("limit", "must be between 1 and " + MAX);
        }
    }

    public static PageSize defaultSize() {
        return new PageSize(DEFAULT);
    }
}
