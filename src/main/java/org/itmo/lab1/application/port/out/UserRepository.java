package org.itmo.lab1.application.port.out;

import java.util.Optional;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.User;
import org.itmo.lab1.domain.user.Username;

public interface UserRepository {

    Optional<User> findByUsername(Username username);

    User create(Username username, PasswordHash passwordHash);
}
