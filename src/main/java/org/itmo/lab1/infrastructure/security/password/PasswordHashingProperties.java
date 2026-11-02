package org.itmo.lab1.infrastructure.security.password;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.password-hashing")
public record PasswordHashingProperties(int maxConcurrent, Duration acquireTimeout) {

    public PasswordHashingProperties {
        if (maxConcurrent < 1 || maxConcurrent > 64) {
            throw new IllegalArgumentException(
                    "Invalid app.security.password-hashing configuration: max-concurrent must be in [1, 64]");
        }
        if (acquireTimeout == null || acquireTimeout.isNegative() || acquireTimeout.compareTo(Duration.ofSeconds(10)) > 0) {
            throw new IllegalArgumentException(
                    "Invalid app.security.password-hashing configuration: acquire-timeout must be in [0, 10s]");
        }
    }
}
