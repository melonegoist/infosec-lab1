package org.itmo.lab1.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.itmo.lab1.domain.DomainValidationException;
import org.junit.jupiter.api.Test;

class PasswordHashTest {

    @Test
    void neverPrintsTheHash() {
        String encoded = "{argon2}$argon2id$v=19$m=19456,t=2,p=1$c2FsdHNhbHQ$aGFzaGhhc2g";

        assertThat(new PasswordHash(encoded).toString()).doesNotContain("argon2id").contains("<redacted>");
        assertThat(new User(new UserId(1), new Username("alice"), new PasswordHash(encoded)).toString())
                .doesNotContain("argon2id");
    }

    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> new PasswordHash(" ")).isInstanceOf(DomainValidationException.class);
    }
}
