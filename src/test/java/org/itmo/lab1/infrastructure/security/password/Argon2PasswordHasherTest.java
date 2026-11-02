package org.itmo.lab1.infrastructure.security.password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.itmo.lab1.application.port.out.PasswordHasherBusyException;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.RawPassword;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class Argon2PasswordHasherTest {

    private static final RawPassword PASSWORD = new RawPassword("correct horse battery staple");

    private final Argon2PasswordHasher hasher =
            new Argon2PasswordHasher(new PasswordHashingProperties(4, Duration.ofSeconds(5)));

    @Test
    void producesArgon2idWithExplicitParametersMatchingTheDatabaseConstraint() {
        assertThat(hasher.hash(PASSWORD).value())
                .startsWith("{argon2}$argon2id$v=19$m=65536,t=3,p=1$")
                .doesNotContain("horse");
    }

    @Test
    void usesFreshRandomSaltForEveryHash() {
        PasswordHash first = hasher.hash(PASSWORD);
        PasswordHash second = hasher.hash(PASSWORD);

        assertThat(first).isNotEqualTo(second);
        assertThat(hasher.matches(PASSWORD, first)).isTrue();
        assertThat(hasher.matches(PASSWORD, second)).isTrue();
    }

    @Test
    void verifiesOnlyTheExactPassword() {
        PasswordHash hash = hasher.hash(PASSWORD);

        assertThat(hasher.matches(new RawPassword("Correct horse battery staple"), hash)).isFalse();
        assertThat(hasher.matches(new RawPassword("correct horse battery staple "), hash)).isFalse();
    }

    @Test
    void failsFastWhenConcurrencyLimitIsExhausted() throws Exception {
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Argon2PasswordHasher limited = new Argon2PasswordHasher(
                blockingEncoder(inside, release), new PasswordHashingProperties(1, Duration.ofMillis(50)));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<PasswordHash> holder = executor.submit(() -> limited.hash(PASSWORD));
            assertThat(inside.await(5, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> limited.matches(PASSWORD, new PasswordHash("{argon2}x")))
                    .isInstanceOf(PasswordHasherBusyException.class);

            release.countDown();
            holder.get(5, TimeUnit.SECONDS);
            assertThat(limited.matches(PASSWORD, new PasswordHash("{argon2}x"))).isFalse();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void releasesPermitEvenWhenHashingFails() {
        PasswordEncoder failing = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                throw new IllegalStateException("boom");
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return false;
            }
        };
        Argon2PasswordHasher limited =
                new Argon2PasswordHasher(failing, new PasswordHashingProperties(1, Duration.ZERO));

        assertThatThrownBy(() -> limited.hash(PASSWORD)).isInstanceOf(IllegalStateException.class);
        assertThat(limited.matches(PASSWORD, new PasswordHash("{argon2}x"))).isFalse();
    }

    private static PasswordEncoder blockingEncoder(CountDownLatch inside, CountDownLatch release) {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                inside.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "{argon2}$argon2id$blocked";
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return false;
            }
        };
    }
}
