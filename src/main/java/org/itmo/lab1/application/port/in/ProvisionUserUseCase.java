package org.itmo.lab1.application.port.in;

import org.itmo.lab1.domain.user.RawPassword;
import org.itmo.lab1.domain.user.Username;

public interface ProvisionUserUseCase {

    boolean ensureUserExists(Username username, RawPassword password);
}
