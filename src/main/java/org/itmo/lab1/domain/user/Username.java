package org.itmo.lab1.domain.user;

import java.util.Locale;
import java.util.regex.Pattern;
import org.itmo.lab1.domain.DomainValidationException;

public record Username(String value) {

    public static final int MAX_LENGTH = 32;

    private static final Pattern FORMAT = Pattern.compile("[a-z0-9._-]{3,32}");

    public Username {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new DomainValidationException("username", "must be 3-32 characters: a-z, 0-9, '.', '_', '-'");
        }
    }

    public static Username normalize(String raw) {
        if (raw == null || raw.length() > MAX_LENGTH) {
            throw new DomainValidationException("username", "must be 3-32 characters: a-z, 0-9, '.', '_', '-'");
        }
        return new Username(raw.toLowerCase(Locale.ROOT));
    }
}
