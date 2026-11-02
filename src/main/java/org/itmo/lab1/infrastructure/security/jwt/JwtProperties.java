package org.itmo.lab1.infrastructure.security.jwt;

import java.time.Duration;
import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        String audience,
        Duration accessTokenTtl,
        Duration clockSkew) {

    public static final int MIN_SECRET_BYTES = 32;

    public static final Duration MAX_ACCESS_TOKEN_TTL = Duration.ofHours(1);

    public static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(1);

    public JwtProperties {
        byte[] key = decodeSecret(secret);
        if (key.length < MIN_SECRET_BYTES) {
            throw invalid("secret must decode to at least %d bytes (256 bits) for HS256, got %d"
                    .formatted(MIN_SECRET_BYTES, key.length));
        }
        requireText(issuer, "issuer");
        requireText(audience, "audience");
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()
                || accessTokenTtl.compareTo(MAX_ACCESS_TOKEN_TTL) > 0) {
            throw invalid("access-token-ttl must be in (0, %s]".formatted(MAX_ACCESS_TOKEN_TTL));
        }
        if (clockSkew == null || clockSkew.isNegative() || clockSkew.compareTo(MAX_CLOCK_SKEW) > 0) {
            throw invalid("clock-skew must be in [0, %s]".formatted(MAX_CLOCK_SKEW));
        }
    }

    public byte[] secretBytes() {
        return decodeSecret(secret);
    }

    @Override
    public String toString() {
        return "JwtProperties[secret=<redacted>, issuer=%s, audience=%s, accessTokenTtl=%s, clockSkew=%s]"
                .formatted(issuer, audience, accessTokenTtl, clockSkew);
    }

    private static byte[] decodeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw invalid("secret is not set (environment variable JWT_SECRET)");
        }
        try {
            return Base64.getDecoder().decode(secret.strip());
        } catch (IllegalArgumentException e) {
            throw invalid("secret must be standard Base64 (generate with: openssl rand -base64 32)");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw invalid(name + " must not be blank");
        }
    }

    private static IllegalArgumentException invalid(String reason) {
        return new IllegalArgumentException("Invalid app.security.jwt configuration: " + reason);
    }
}
