package org.itmo.lab1.infrastructure.persistence.user;

import java.util.Optional;
import org.itmo.lab1.application.port.out.UserRepository;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.User;
import org.itmo.lab1.domain.user.UserId;
import org.itmo.lab1.domain.user.Username;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaUserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository users;

    public JpaUserRepositoryAdapter(UserJpaRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(Username username) {
        return users.findByUsername(username.value()).map(JpaUserRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public User create(Username username, PasswordHash passwordHash) {
        return toDomain(users.save(new UserEntity(username.value(), passwordHash.value())));
    }

    static User toDomain(UserEntity entity) {
        return new User(new UserId(entity.getId()), new Username(entity.getUsername()),
                new PasswordHash(entity.getPasswordHash()));
    }
}
