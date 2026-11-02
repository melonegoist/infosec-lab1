package org.itmo.lab1.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.itmo.lab1.application.port.out.PasswordHasher;
import org.itmo.lab1.application.port.out.UserRepository;
import org.itmo.lab1.domain.DomainValidationException;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.RawPassword;
import org.itmo.lab1.domain.user.User;
import org.itmo.lab1.domain.user.UserId;
import org.itmo.lab1.domain.user.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    private static final Username DEMO = new Username("demo");
    private static final RawPassword STRONG = new RawPassword("correct horse battery staple");
    private static final PasswordHash HASH = new PasswordHash("{argon2}$argon2id$hash");

    @Mock
    private UserRepository users;

    @Mock
    private PasswordHasher hasher;

    private UserProvisioningService service;

    @BeforeEach
    void createService() {
        service = new UserProvisioningService(users, hasher);
    }

    @Test
    void createsUserWithHashedPasswordWhenAbsent() {
        when(users.findByUsername(DEMO)).thenReturn(Optional.empty());
        when(hasher.hash(STRONG)).thenReturn(HASH);

        assertThat(service.ensureUserExists(DEMO, STRONG)).isTrue();
        verify(users).create(DEMO, HASH);
    }

    @Test
    void leavesExistingUserUntouched() {
        when(users.findByUsername(DEMO)).thenReturn(Optional.of(new User(new UserId(1), DEMO, HASH)));

        assertThat(service.ensureUserExists(DEMO, STRONG)).isFalse();
        verify(users, never()).create(any(), any());
        verifyNoInteractions(hasher);
    }

    @Test
    void rejectsWeakPasswordBeforeHashingOrTouchingStorage() {
        assertThatThrownBy(() -> service.ensureUserExists(DEMO, new RawPassword("short")))
                .isInstanceOf(DomainValidationException.class);
        verifyNoInteractions(users, hasher);
    }
}
