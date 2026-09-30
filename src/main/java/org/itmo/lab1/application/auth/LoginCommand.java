package org.itmo.lab1.application.auth;

import java.util.Objects;
import org.itmo.lab1.domain.user.RawPassword;
import org.itmo.lab1.domain.user.Username;

public record LoginCommand(Username username, RawPassword password) {

    public LoginCommand {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
    }
}
