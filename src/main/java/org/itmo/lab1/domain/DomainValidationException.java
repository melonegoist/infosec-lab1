package org.itmo.lab1.domain;

public class DomainValidationException extends RuntimeException {

    private final String field;

    public DomainValidationException(String field, String rule) {
        super(field + " " + rule);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
