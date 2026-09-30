package org.itmo.lab1.application.auth;

import java.time.Instant;
import java.util.Objects;

public record IssuedToken(String value, Instant expiresAt) {

    public IssuedToken {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    @Override
    public String toString() {
        return "IssuedToken[value=<redacted>, expiresAt=" + expiresAt + "]";
    }
}
