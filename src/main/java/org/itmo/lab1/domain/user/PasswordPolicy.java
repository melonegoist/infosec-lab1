package org.itmo.lab1.domain.user;

import java.util.Locale;
import org.itmo.lab1.domain.DomainValidationException;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 15;

    private PasswordPolicy() {
    }

    public static void requireAcceptableForNewAccount(RawPassword password, Username username) {
        String value = password.value();
        if (value.codePointCount(0, value.length()) < MIN_LENGTH) {
            throw new DomainValidationException("password", "must be at least " + MIN_LENGTH + " characters");
        }
        if (value.toLowerCase(Locale.ROOT).contains(username.value())) {
            throw new DomainValidationException("password", "must not contain the username");
        }
    }
}
