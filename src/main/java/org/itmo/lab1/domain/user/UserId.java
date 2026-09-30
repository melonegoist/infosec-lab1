package org.itmo.lab1.domain.user;

import org.itmo.lab1.domain.DomainValidationException;

public record UserId(long value) {

    public UserId {
        if (value <= 0) {
            throw new DomainValidationException("userId", "must be positive");
        }
    }
}
