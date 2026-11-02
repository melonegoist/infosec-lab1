package org.itmo.lab1.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Optional;
import org.itmo.lab1.application.port.out.UserRepository;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.RawPassword;
import org.itmo.lab1.domain.user.User;
import org.itmo.lab1.domain.user.UserId;
import org.itmo.lab1.domain.user.Username;
import org.itmo.lab1.infrastructure.security.password.Argon2PasswordHasher;
import org.itmo.lab1.infrastructure.security.password.PasswordHashingProperties;
import org.junit.jupiter.api.Test;

class LoginTimingTest {

    private static final RawPassword REAL_PASSWORD = new RawPassword("correct horse battery staple");
    private static final RawPassword WRONG_PASSWORD = new RawPassword("wrong password, definitely");

    @Test
    void unknownUserTakesAboutAsLongAsWrongPassword() {
        Argon2PasswordHasher hasher = new Argon2PasswordHasher(new PasswordHashingProperties(4, Duration.ofSeconds(5)));
        User alice = new User(new UserId(1), new Username("alice"), hasher.hash(REAL_PASSWORD));
        LoginService service = new LoginService(singleUser(alice), hasher,
                user -> { throw new AssertionError("token must not be issued"); });

        long unknownUserNanos = 0;
        long wrongPasswordNanos = 0;
        for (int round = 0; round < 4; round++) {
            unknownUserNanos += timeFailedLogin(service, new Username("mallory"));
            wrongPasswordNanos += timeFailedLogin(service, alice.username());
        }

        assertThat((double) unknownUserNanos / wrongPasswordNanos).isBetween(0.5, 2.0);
    }

    private static long timeFailedLogin(LoginService service, Username username) {
        long start = System.nanoTime();
        assertThatThrownBy(() -> service.login(new LoginCommand(username, WRONG_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
        return System.nanoTime() - start;
    }

    private static UserRepository singleUser(User user) {
        return new UserRepository() {
            @Override
            public Optional<User> findByUsername(Username username) {
                return user.username().equals(username) ? Optional.of(user) : Optional.empty();
            }

            @Override
            public User create(Username username, PasswordHash passwordHash) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
