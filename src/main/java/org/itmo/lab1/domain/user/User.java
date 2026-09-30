package org.itmo.lab1.domain.user;

import java.util.Objects;

public record User(UserId id, Username username, PasswordHash passwordHash) {

    public User {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(passwordHash, "passwordHash");
    }
}
