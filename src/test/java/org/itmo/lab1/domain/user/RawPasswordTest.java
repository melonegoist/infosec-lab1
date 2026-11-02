package org.itmo.lab1.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.itmo.lab1.application.auth.LoginCommand;
import org.itmo.lab1.domain.DomainValidationException;
import org.junit.jupiter.api.Test;

class RawPasswordTest {

    @Test
    void nfkcMakesCanonicallyEquivalentInputsEqual() {
        String composed = "caf\u00E9 au lait 2026";
        String decomposed = "cafe\u0301 au lait 2026";

        assertThat(composed).isNotEqualTo(decomposed);
        assertThat(RawPassword.normalize(composed)).isEqualTo(RawPassword.normalize(decomposed));
    }

    @Test
    void nfkcFoldsCompatibilityCharacters() {
        assertThat(RawPassword.normalize("\uFF50\uFF41\uFF53\uFF53").value()).isEqualTo("pass");
        assertThat(RawPassword.normalize("\uFB01le").value()).isEqualTo("file");
    }

    @Test
    void keepsWhitespaceAndNeverTruncates() {
        assertThat(RawPassword.normalize("  correct horse  ").value()).isEqualTo("  correct horse  ");
        assertThat(RawPassword.normalize("a".repeat(RawPassword.MAX_LENGTH)).value()).hasSize(128);

        assertThatThrownBy(() -> RawPassword.normalize("a".repeat(RawPassword.MAX_LENGTH + 1)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> RawPassword.normalize("")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void lengthIsRecheckedAfterNormalizationExpandsTheInput() {
        String expanding = "\uFDFA".repeat(8);

        assertThatThrownBy(() -> RawPassword.normalize(expanding)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void neverPrintsThePassword() {
        RawPassword password = new RawPassword("correct horse battery staple");

        assertThat(password.toString()).doesNotContain("horse");
        assertThat(new LoginCommand(new Username("alice"), password).toString()).doesNotContain("horse");
    }
}
