package org.itmo.lab1.infrastructure.security;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public final class AuthenticatedUserToken extends AbstractAuthenticationToken {

    @Serial
    private static final long serialVersionUID = 1L;

    private final AuthenticatedUser principal;

    public AuthenticatedUserToken(AuthenticatedUser principal) {
        super(List.of());
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return principal;
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws NotSerializableException {
        throw new NotSerializableException("stateless authentication must not be serialized");
    }

    @Serial
    private void readObject(ObjectInputStream in) throws NotSerializableException {
        throw new NotSerializableException("stateless authentication must not be deserialized");
    }
}
