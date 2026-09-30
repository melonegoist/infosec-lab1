package org.itmo.lab1.application.port.out;

public class UnknownAuthorException extends RuntimeException {

    public UnknownAuthorException() {
        super("author does not exist");
    }
}
