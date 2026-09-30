package org.itmo.lab1.application.port.out;

import org.itmo.lab1.application.auth.IssuedToken;
import org.itmo.lab1.domain.user.User;

public interface TokenIssuer {

    IssuedToken issue(User user);
}
