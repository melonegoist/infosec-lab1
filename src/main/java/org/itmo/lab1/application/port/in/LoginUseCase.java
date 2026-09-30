package org.itmo.lab1.application.port.in;

import org.itmo.lab1.application.auth.IssuedToken;
import org.itmo.lab1.application.auth.LoginCommand;

public interface LoginUseCase {

    IssuedToken login(LoginCommand command);
}
