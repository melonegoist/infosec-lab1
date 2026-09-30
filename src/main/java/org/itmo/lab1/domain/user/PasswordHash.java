package org.itmo.lab1.domain.user;

import org.itmo.lab1.domain.DomainValidationException;

public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("passwordHash", "must not be blank");
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[<redacted>]";
    }
}
