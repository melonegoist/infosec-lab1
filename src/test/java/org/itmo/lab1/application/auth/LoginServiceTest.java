package org.itmo.lab1.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.itmo.lab1.application.port.out.PasswordHasher;
import org.itmo.lab1.application.port.out.PasswordHasherBusyException;
import org.itmo.lab1.application.port.out.TokenIssuer;
import org.itmo.lab1.application.port.out.UserRepository;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.RawPassword;
import org.itmo.lab1.domain.user.User;
import org.itmo.lab1.domain.user.UserId;
import org.itmo.lab1.domain.user.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final PasswordHash DUMMY_HASH = new PasswordHash("{argon2}$argon2id$dummy");
    private static final PasswordHash ALICE_HASH = new PasswordHash("{argon2}$argon2id$alice");
    private static final User ALICE = new User(new UserId(7), new Username("alice"), ALICE_HASH);
    private static final RawPassword PASSWORD = new RawPassword("correct horse battery staple");
    private static final IssuedToken TOKEN = new IssuedToken("token", Instant.parse("2030-01-01T00:00:00Z"));

    @Mock
    private UserRepository users;

    @Mock
    private PasswordHasher hasher;

    @Mock
    private TokenIssuer tokens;

    private LoginService service;

    @BeforeEach
    void createService() {
        when(hasher.hash(any(RawPassword.class))).thenReturn(DUMMY_HASH);
        service = new LoginService(users, hasher, tokens);
    }

    @Test
    void issuesTokenForValidCredentials() {
        when(users.findByUsername(ALICE.username())).thenReturn(Optional.of(ALICE));
        when(hasher.matches(PASSWORD, ALICE_HASH)).thenReturn(true);
        when(tokens.issue(ALICE)).thenReturn(TOKEN);

        assertThat(service.login(new LoginCommand(ALICE.username(), PASSWORD))).isEqualTo(TOKEN);
    }

    @Test
    void rejectsWrongPasswordWithoutIssuingToken() {
        when(users.findByUsername(ALICE.username())).thenReturn(Optional.of(ALICE));
        when(hasher.matches(PASSWORD, ALICE_HASH)).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand(ALICE.username(), PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(tokens, never()).issue(any());
    }

    @Test
    void unknownUserStillPaysForExactlyOneVerificationAgainstDummyHash() {
        Username mallory = new Username("mallory");
        when(users.findByUsername(mallory)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginCommand(mallory, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(hasher, times(1)).matches(PASSWORD, DUMMY_HASH);
        verify(tokens, never()).issue(any());
    }

    @Test
    void unknownUserAndWrongPasswordAreIndistinguishable() {
        Username mallory = new Username("mallory");
        when(users.findByUsername(mallory)).thenReturn(Optional.empty());
        when(users.findByUsername(ALICE.username())).thenReturn(Optional.of(ALICE));
        when(hasher.matches(any(), any())).thenReturn(false);

        Throwable unknownUser = catchLogin(new LoginCommand(mallory, PASSWORD));
        Throwable wrongPassword = catchLogin(new LoginCommand(ALICE.username(), PASSWORD));

        assertThat(unknownUser).isExactlyInstanceOf(wrongPassword.getClass())
                .hasMessage(wrongPassword.getMessage());
    }

    @Test
    void busyHasherPropagatesAndNoTokenIsIssued() {
        when(users.findByUsername(ALICE.username())).thenReturn(Optional.of(ALICE));
        when(hasher.matches(PASSWORD, ALICE_HASH)).thenThrow(new PasswordHasherBusyException());

        assertThatThrownBy(() -> service.login(new LoginCommand(ALICE.username(), PASSWORD)))
                .isInstanceOf(PasswordHasherBusyException.class);
        verify(tokens, never()).issue(any());
    }

    private Throwable catchLogin(LoginCommand command) {
        try {
            service.login(command);
            throw new AssertionError("login must fail");
        } catch (InvalidCredentialsException e) {
            return e;
        }
    }
}
