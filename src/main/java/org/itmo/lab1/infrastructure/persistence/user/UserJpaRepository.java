package org.itmo.lab1.infrastructure.persistence.user;

import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface UserJpaRepository extends Repository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    UserEntity save(UserEntity user);

    UserEntity getReferenceById(Long id);
}
