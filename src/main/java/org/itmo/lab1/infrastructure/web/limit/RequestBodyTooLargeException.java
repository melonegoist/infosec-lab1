package org.itmo.lab1.infrastructure.web.limit;

public class RequestBodyTooLargeException extends RuntimeException {

    public RequestBodyTooLargeException() {
        super("request body exceeds the configured limit");
    }
}
