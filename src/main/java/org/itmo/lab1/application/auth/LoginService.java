package org.itmo.lab1.application.auth;

import java.util.Optional;
import java.util.UUID;
import org.itmo.lab1.application.port.in.LoginUseCase;
import org.itmo.lab1.application.port.out.PasswordHasher;
import org.itmo.lab1.application.port.out.TokenIssuer;
import org.itmo.lab1.application.port.out.UserRepository;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.RawPassword;
import org.itmo.lab1.domain.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoginService implements LoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final TokenIssuer tokens;

    private final PasswordHash dummyHash;

    public LoginService(UserRepository users, PasswordHasher hasher, TokenIssuer tokens) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.dummyHash = hasher.hash(new RawPassword(UUID.randomUUID().toString()));
    }

    @Override
    public IssuedToken login(LoginCommand command) {
        Optional<User> user = users.findByUsername(command.username());

        boolean passwordMatches = hasher.matches(command.password(),
                user.map(User::passwordHash).orElse(dummyHash));

        if (user.isEmpty() || !passwordMatches) {
            log.info("Login failed: username={}", command.username().value());
            throw new InvalidCredentialsException();
        }
        log.info("Login succeeded: userId={}", user.get().id().value());
        return tokens.issue(user.get());
    }
}
