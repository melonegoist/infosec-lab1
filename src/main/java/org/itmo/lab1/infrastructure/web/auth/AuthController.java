package org.itmo.lab1.infrastructure.web.auth;

import java.time.Clock;
import java.time.Duration;
import org.itmo.lab1.application.auth.IssuedToken;
import org.itmo.lab1.application.auth.LoginCommand;
import org.itmo.lab1.application.port.in.LoginUseCase;
import org.itmo.lab1.domain.user.RawPassword;
import org.itmo.lab1.domain.user.Username;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final Clock clock;

    public AuthController(LoginUseCase loginUseCase, Clock clock) {
        this.loginUseCase = loginUseCase;
        this.clock = clock;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TokenResponse login(@RequestBody LoginRequest request) {
        IssuedToken token = loginUseCase.login(new LoginCommand(
                Username.normalize(request.username()),
                RawPassword.normalize(request.password())));
        long expiresIn = Math.max(0, Duration.between(clock.instant(), token.expiresAt()).toSeconds());
        return TokenResponse.bearer(token.value(), expiresIn);
    }
}
