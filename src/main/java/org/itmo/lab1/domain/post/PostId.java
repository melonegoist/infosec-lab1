package org.itmo.lab1.domain.post;

import org.itmo.lab1.domain.DomainValidationException;

public record PostId(long value) {

    public PostId {
        if (value <= 0) {
            throw new DomainValidationException("postId", "must be positive");
        }
    }
}
