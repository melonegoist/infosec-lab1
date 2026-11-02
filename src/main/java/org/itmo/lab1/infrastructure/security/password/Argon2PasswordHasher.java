package org.itmo.lab1.infrastructure.security.password;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.itmo.lab1.application.port.out.PasswordHasher;
import org.itmo.lab1.application.port.out.PasswordHasherBusyException;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.RawPassword;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2PasswordHasher implements PasswordHasher {

    static final int SALT_LENGTH_BYTES = 16;
    static final int HASH_LENGTH_BYTES = 32;
    static final int PARALLELISM = 1;
    static final int MEMORY_KIB = 65_536;
    static final int ITERATIONS = 3;

    private static final String ENCODER_ID = "argon2";

    private final PasswordEncoder encoder;
    private final Semaphore permits;
    private final long acquireTimeoutMillis;

    @Autowired
    public Argon2PasswordHasher(PasswordHashingProperties properties) {
        this(new DelegatingPasswordEncoder(ENCODER_ID, Map.of(ENCODER_ID, new Argon2PasswordEncoder(
                SALT_LENGTH_BYTES, HASH_LENGTH_BYTES, PARALLELISM, MEMORY_KIB, ITERATIONS))), properties);
    }

    Argon2PasswordHasher(PasswordEncoder encoder, PasswordHashingProperties properties) {
        this.encoder = encoder;
        this.permits = new Semaphore(properties.maxConcurrent(), true);
        this.acquireTimeoutMillis = properties.acquireTimeout().toMillis();
    }

    @Override
    public PasswordHash hash(RawPassword password) {
        return new PasswordHash(withPermit(() -> encoder.encode(password.value())));
    }

    @Override
    public boolean matches(RawPassword password, PasswordHash hash) {
        return withPermit(() -> encoder.matches(password.value(), hash.value()));
    }

    private <T> T withPermit(Supplier<T> work) {
        try {
            if (!permits.tryAcquire(acquireTimeoutMillis, TimeUnit.MILLISECONDS)) {
                throw new PasswordHasherBusyException();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PasswordHasherBusyException();
        }
        try {
            return work.get();
        } finally {
            permits.release();
        }
    }
}
