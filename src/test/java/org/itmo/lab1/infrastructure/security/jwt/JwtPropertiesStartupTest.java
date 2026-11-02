package org.itmo.lab1.infrastructure.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.itmo.lab1.support.TestSecrets.randomBase64Key;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

@ExtendWith(OutputCaptureExtension.class)
class JwtPropertiesStartupTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class JwtPropertiesOnly {
    }

    @Test
    void validKeyBinds() {
        try (ConfigurableApplicationContext ctx = start("--app.security.jwt.secret=" + randomBase64Key(32))) {
            assertThat(ctx.getBean(JwtProperties.class).secretBytes()).hasSize(32);
        }
    }

    @Test
    void weakKeyStopsStartupAndIsNeverPrinted(CapturedOutput output) {
        String weakKey = randomBase64Key(16);

        assertThatThrownBy(() -> start("--app.security.jwt.secret=" + weakKey))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);

        assertThat(output.getAll())
                .contains("at least 32 bytes")
                .doesNotContain(weakKey);
    }

    @Test
    void missingKeyStopsStartup(CapturedOutput output) {
        assertThatThrownBy(JwtPropertiesStartupTest::start)
                .hasRootCauseInstanceOf(IllegalArgumentException.class);

        assertThat(output.getAll()).contains("JWT_SECRET");
    }

    private static ConfigurableApplicationContext start(String... extraArgs) {
        String[] args = Stream.concat(Stream.of(
                        "--spring.config.name=jwt-properties-startup-test",
                        "--app.security.jwt.issuer=lab1-auth",
                        "--app.security.jwt.audience=lab1-api",
                        "--app.security.jwt.access-token-ttl=15m",
                        "--app.security.jwt.clock-skew=30s"),
                Stream.of(extraArgs)).toArray(String[]::new);
        return new SpringApplicationBuilder(JwtPropertiesOnly.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
