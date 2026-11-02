package org.itmo.lab1.infrastructure.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class BootstrapUserPropertiesTest {

    private static final String SECRET = "correct horse battery staple";

    @Test
    void disabledWhenNothingConfigured() {
        assertThat(new BootstrapUserProperties(null, null).enabled()).isFalse();
        assertThat(new BootstrapUserProperties("", " ").enabled()).isFalse();
    }

    @Test
    void enabledWhenBothConfigured() {
        assertThat(new BootstrapUserProperties("demo", SECRET).enabled()).isTrue();
    }

    @Test
    void halfConfiguredIsAnErrorThatDoesNotEchoValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> new BootstrapUserProperties("demo", ""));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BootstrapUserProperties(null, SECRET))
                .withMessageNotContaining(SECRET);
    }

    @Test
    void neverPrintsThePassword() {
        assertThat(new BootstrapUserProperties("demo", SECRET).toString())
                .contains("demo")
                .doesNotContain(SECRET);
    }
}
