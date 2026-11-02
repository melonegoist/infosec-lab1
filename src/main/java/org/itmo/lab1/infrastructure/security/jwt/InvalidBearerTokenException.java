package org.itmo.lab1.infrastructure.security.jwt;

import org.springframework.security.core.AuthenticationException;

public class InvalidBearerTokenException extends AuthenticationException {

    public InvalidBearerTokenException() {
        super("invalid bearer token");
    }
}
