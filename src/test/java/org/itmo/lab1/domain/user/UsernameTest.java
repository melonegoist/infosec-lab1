package org.itmo.lab1.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import org.itmo.lab1.domain.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UsernameTest {

    @Test
    void normalizesCaseBeforeValidation() {
        assertThat(Username.normalize("Alice.Smith_1").value()).isEqualTo("alice.smith_1");
    }

    @Test
    void normalizationDoesNotDependOnJvmLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat("ADMIN".toLowerCase()).isEqualTo("adm\u0131n");
            assertThat(Username.normalize("ADMIN").value()).isEqualTo("admin");
        } finally {
            Locale.setDefault(original);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ab",
            "abcdefghijklmnopqrstuvwxyz0123456",
            "alice smith",
            "alice'--",
            "' OR '1'='1",
            "<script>alert(1)</script>",
            "\u0430dmin",
            "admin\n",
            "admin\u0000"
    })
    void rejectsAnythingOutsideTheAllowlistWithoutEchoingIt(String raw) {
        assertThatThrownBy(() -> Username.normalize(raw))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageNotContaining(raw)
                .extracting(e -> ((DomainValidationException) e).field()).isEqualTo("username");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsMissingValue(String raw) {
        assertThatThrownBy(() -> Username.normalize(raw)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorDoesNotNormalizeSoStoredDataMustAlreadyBeCanonical() {
        assertThatThrownBy(() -> new Username("Admin")).isInstanceOf(DomainValidationException.class);
    }
}
