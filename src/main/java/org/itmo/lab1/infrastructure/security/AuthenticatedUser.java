package org.itmo.lab1.infrastructure.security;

import java.util.Objects;
import org.itmo.lab1.domain.user.UserId;

public record AuthenticatedUser(UserId id) {

    public AuthenticatedUser {
        Objects.requireNonNull(id, "id");
    }
}
