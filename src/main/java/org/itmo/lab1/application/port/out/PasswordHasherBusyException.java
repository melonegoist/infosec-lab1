package org.itmo.lab1.application.port.out;

public class PasswordHasherBusyException extends RuntimeException {

    public PasswordHasherBusyException() {
        super("password hashing capacity exhausted");
    }
}
