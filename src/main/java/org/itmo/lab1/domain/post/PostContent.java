package org.itmo.lab1.domain.post;

import org.itmo.lab1.domain.DomainValidationException;

public record PostContent(String value) {

    public static final int MAX_LENGTH = 2000;

    public PostContent {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("content", "must not be blank");
        }
        if (value.length() > 2 * MAX_LENGTH || value.codePointCount(0, value.length()) > MAX_LENGTH) {
            throw new DomainValidationException("content", "must be at most " + MAX_LENGTH + " characters");
        }
        if (!value.codePoints().allMatch(PostContent::isAllowed)) {
            throw new DomainValidationException("content", "contains control or bidirectional-override characters");
        }
    }

    private static boolean isAllowed(int codePoint) {
        return switch (codePoint) {
            case '\n', '\r', '\t' -> true;
            default -> !Character.isISOControl(codePoint)
                    && !(codePoint >= 0x202A && codePoint <= 0x202E)
                    && !(codePoint >= 0x2066 && codePoint <= 0x2069)
                    && Character.getType(codePoint) != Character.SURROGATE;
        };
    }
}
