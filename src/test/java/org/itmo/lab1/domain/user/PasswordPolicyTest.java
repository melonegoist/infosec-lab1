package org.itmo.lab1.domain.user;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.itmo.lab1.domain.DomainValidationException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private static final Username ALICE = new Username("alice");
    private static final String EMOJI = "\uD83D\uDD12";

    @Test
    void requiresFifteenCodePoints() {
        assertThatThrownBy(() -> check("a".repeat(14))).isInstanceOf(DomainValidationException.class);
        assertThatNoException().isThrownBy(() -> check("a".repeat(15)));
    }

    @Test
    void countsCodePointsNotUtf16Units() {
        assertThatThrownBy(() -> check(EMOJI.repeat(14))).isInstanceOf(DomainValidationException.class);
        assertThatNoException().isThrownBy(() -> check(EMOJI.repeat(15)));
    }

    @Test
    void hasNoCompositionRulesPerNist() {
        assertThatNoException().isThrownBy(() -> check("correct horse battery staple"));
    }

    @Test
    void rejectsPasswordContainingUsernameCaseInsensitively() {
        assertThatThrownBy(() -> check("my-ALICE-password-2026"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("username");
    }

    private static void check(String password) {
        PasswordPolicy.requireAcceptableForNewAccount(new RawPassword(password), ALICE);
    }
}
