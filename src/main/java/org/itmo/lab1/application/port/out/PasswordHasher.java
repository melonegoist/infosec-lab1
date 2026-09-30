package org.itmo.lab1.application.port.out;

import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.RawPassword;

public interface PasswordHasher {

    PasswordHash hash(RawPassword password);

    boolean matches(RawPassword password, PasswordHash hash);
}
