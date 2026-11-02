package org.itmo.lab1.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;
import org.itmo.lab1.application.port.out.UserRepository;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.User;
import org.itmo.lab1.domain.user.Username;
import org.itmo.lab1.infrastructure.persistence.user.UserEntity;
import org.itmo.lab1.infrastructure.persistence.user.UserJpaRepository;
import org.itmo.lab1.support.PostgresJpaTest;
import org.itmo.lab1.support.SqlCapture;
import org.itmo.lab1.support.SqlStates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@PostgresJpaTest
@ExtendWith(OutputCaptureExtension.class)
class UserPersistenceTest {

    private static final PasswordHash HASH =
            new PasswordHash("{argon2}$argon2id$v=19$m=19456,t=2,p=1$c2FsdHNhbHQ$aGFzaGhhc2g");

    @Autowired
    private UserRepository users;

    @Autowired
    private UserJpaRepository jpaUsers;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createsAndFindsUserByNormalizedName() {
        User created = users.create(new Username("alice"), HASH);

        assertThat(users.findByUsername(Username.normalize("ALICE")))
                .hasValueSatisfying(found -> {
                    assertThat(found.id()).isEqualTo(created.id());
                    assertThat(found.passwordHash()).isEqualTo(HASH);
                });
        assertThat(users.findByUsername(new Username("bob"))).isEmpty();
    }

    @Test
    void injectionPayloadIsBoundAsDataAndNeverReachesSqlText() {
        users.create(new Username("alice"), HASH);
        String payload = "alice' OR '1'='1";
        SqlCapture.clear();

        assertThat(jpaUsers.findByUsername(payload)).isEmpty();

        assertThat(SqlCapture.statements()).singleElement().satisfies(sql -> assertThat(sql)
                .contains("username=?")
                .doesNotContain("'1'='1"));
    }

    @Test
    void positiveControlSamePayloadIsExploitableWhenConcatenated() {
        users.create(new Username("alice"), HASH);
        String payload = "alice' OR '1'='1";

        var leaked = entityManager
                .createQuery("select u from UserEntity u where u.username = '" + payload + "'", UserEntity.class)
                .getResultList();

        assertThat(leaked).extracting(UserEntity::getUsername).contains("alice");
    }

    @Test
    void databaseRejectsNonCanonicalUsernameEvenWhenDomainIsBypassed() {
        assertThatThrownBy(() -> jpaUsers.save(new UserEntity("Bob Smith", HASH.value())))
                .satisfies(e -> assertThat(SqlStates.of(e)).isEqualTo(SqlStates.CHECK_VIOLATION));
    }

    @Test
    void databaseRejectsPlaintextPasswordInHashColumnWithoutLeakingIt(CapturedOutput output) {
        String plaintext = "hunter2-" + UUID.randomUUID();

        assertThatThrownBy(() -> jpaUsers.save(new UserEntity("bob", plaintext)))
                .satisfies(e -> assertThat(SqlStates.of(e)).isEqualTo(SqlStates.CHECK_VIOLATION))
                .satisfies(e -> assertThat(fullTrace(e)).doesNotContain(plaintext));

        assertThat(output.getAll()).doesNotContain(plaintext);
    }

    private static String fullTrace(Throwable error) {
        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        return trace.toString();
    }

    @Test
    void usernamesAreUnique() {
        users.create(new Username("alice"), HASH);

        assertThatThrownBy(() -> users.create(new Username("alice"), HASH))
                .satisfies(e -> assertThat(SqlStates.of(e)).isEqualTo(SqlStates.UNIQUE_VIOLATION));
    }
}
