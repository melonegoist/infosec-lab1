package org.itmo.lab1.infrastructure.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.itmo.lab1.support.TestSecrets.randomBase64Key;

import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JwtPropertiesTest {

    private static final Duration TTL = Duration.ofMinutes(15);
    private static final Duration SKEW = Duration.ofSeconds(30);

    private static JwtProperties props(String secret, Duration ttl, Duration skew) {
        return new JwtProperties(secret, "lab1-auth", "lab1-api", ttl, skew);
    }

    @Test
    void acceptsRandom256BitKey() {
        assertThat(props(randomBase64Key(32), TTL, SKEW).secretBytes()).hasSize(32);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void rejectsMissingSecret(String secret) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> props(secret, TTL, SKEW))
                .withMessageContaining("JWT_SECRET");
    }

    @Test
    void rejectsKeyShorterThan256BitsWithoutRevealingIt() {
        String secret = randomBase64Key(31);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> props(secret, TTL, SKEW))
                .withMessageContaining("got 31")
                .withMessageNotContaining(secret);
    }

    @Test
    void rejectsNonBase64SecretWithoutRevealingItOrDecoderCause() {
        String secret = "not-base64-" + randomBase64Key(32);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> props(secret, TTL, SKEW))
                .withMessageNotContaining(secret)
                .withNoCause();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT0S", "-PT1M", "PT1H0.001S"})
    void rejectsAccessTokenTtlOutsideBounds(String ttl) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> props(randomBase64Key(32), Duration.parse(ttl), SKEW))
                .withMessageContaining("access-token-ttl");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-PT1S", "PT1M0.001S"})
    void rejectsClockSkewOutsideBounds(String skew) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> props(randomBase64Key(32), TTL, Duration.parse(skew)))
                .withMessageContaining("clock-skew");
    }

    @Test
    void acceptsBoundaryValues() {
        JwtProperties p = props(randomBase64Key(32), JwtProperties.MAX_ACCESS_TOKEN_TTL, JwtProperties.MAX_CLOCK_SKEW);

        assertThat(p.accessTokenTtl()).isEqualTo(Duration.ofHours(1));
        assertThat(p.clockSkew()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void rejectsMissingDurationsAndClaims() {
        String key = randomBase64Key(32);

        assertThatIllegalArgumentException().isThrownBy(() -> props(key, null, SKEW));
        assertThatIllegalArgumentException().isThrownBy(() -> props(key, TTL, null));
        assertThatIllegalArgumentException().isThrownBy(() -> new JwtProperties(key, " ", "lab1-api", TTL, SKEW));
        assertThatIllegalArgumentException().isThrownBy(() -> new JwtProperties(key, "lab1-auth", null, TTL, SKEW));
    }

    @Test
    void toStringNeverContainsSecret() {
        String secret = randomBase64Key(32);

        assertThat(props(secret, TTL, SKEW).toString())
                .doesNotContain(secret)
                .contains("<redacted>");
    }

    @Test
    void secretBytesReturnsDefensiveCopy() {
        JwtProperties p = props(randomBase64Key(32), TTL, SKEW);
        byte[] leaked = p.secretBytes();

        Arrays.fill(leaked, (byte) 0);

        assertThat(p.secretBytes()).isNotEqualTo(leaked);
    }
}
