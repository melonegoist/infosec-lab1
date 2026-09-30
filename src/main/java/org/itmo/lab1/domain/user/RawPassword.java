package org.itmo.lab1.domain.user;

import java.text.Normalizer;
import org.itmo.lab1.domain.DomainValidationException;

public record RawPassword(String value) {

    public static final int MAX_LENGTH = 128;

    public RawPassword {
        if (value == null || value.isEmpty()) {
            throw new DomainValidationException("password", "must not be empty");
        }
        if (value.length() > MAX_LENGTH) {
            throw new DomainValidationException("password", "must be at most " + MAX_LENGTH + " characters");
        }
    }

    public static RawPassword normalize(String raw) {
        if (raw == null || raw.length() > MAX_LENGTH) {
            throw new DomainValidationException("password", "must be 1-" + MAX_LENGTH + " characters");
        }
        return new RawPassword(Normalizer.normalize(raw, Normalizer.Form.NFKC));
    }

    @Override
    public String toString() {
        return "RawPassword[<redacted>]";
    }
}
