package org.itmo.lab1.application.user;

import org.itmo.lab1.application.port.in.ProvisionUserUseCase;
import org.itmo.lab1.application.port.out.PasswordHasher;
import org.itmo.lab1.application.port.out.UserRepository;
import org.itmo.lab1.domain.user.PasswordPolicy;
import org.itmo.lab1.domain.user.RawPassword;
import org.itmo.lab1.domain.user.Username;

public final class UserProvisioningService implements ProvisionUserUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;

    public UserProvisioningService(UserRepository users, PasswordHasher hasher) {
        this.users = users;
        this.hasher = hasher;
    }

    @Override
    public boolean ensureUserExists(Username username, RawPassword password) {
        PasswordPolicy.requireAcceptableForNewAccount(password, username);
        if (users.findByUsername(username).isPresent()) {
            return false;
        }
        users.create(username, hasher.hash(password));
        return true;
    }
}
