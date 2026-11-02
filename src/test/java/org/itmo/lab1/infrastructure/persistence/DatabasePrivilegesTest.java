package org.itmo.lab1.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.itmo.lab1.support.PostgresJpaTest;
import org.itmo.lab1.support.SqlStates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@PostgresJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DatabasePrivilegesTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Value("${spring.datasource.username}")
    private String appRole;

    @Value("${spring.flyway.user}")
    private String ownerRole;

    @ParameterizedTest
    @ValueSource(strings = {
            "DELETE FROM lab1.users",
            "UPDATE lab1.users SET password_hash = '{argon2}$argon2id$attacker'",
            "TRUNCATE lab1.posts",
            "DROP TABLE lab1.posts",
            "ALTER TABLE lab1.users ADD COLUMN is_admin boolean",
            "CREATE TABLE lab1.backdoor (id int)",
            "CREATE TABLE public.backdoor (id int)",
            "SELECT * FROM lab1.flyway_schema_history",
            "CREATE ROLE attacker LOGIN SUPERUSER",
            "COPY (SELECT 1) TO PROGRAM 'id'",
            "SELECT pg_read_file('/etc/passwd')"
    })
    void runtimeRoleIsDeniedEverythingBeyondItsUseCases(String sql) {
        assertThatThrownBy(() -> jdbc.execute(sql))
                .satisfies(e -> assertThat(SqlStates.of(e)).isEqualTo(SqlStates.INSUFFICIENT_PRIVILEGE));
    }

    @Test
    void applicationConnectsAsUnprivilegedRoleSeparateFromSchemaOwner() {
        assertThat(jdbc.queryForObject("SELECT current_user", String.class))
                .isEqualTo(appRole)
                .isNotEqualTo(ownerRole);
        assertThat(jdbc.queryForObject(
                "SELECT rolsuper OR rolcreaterole OR rolcreatedb OR rolbypassrls FROM pg_roles WHERE rolname = current_user",
                Boolean.class)).isFalse();
    }

    @Test
    void serverSideTimeoutsBoundExpensiveQueries() {
        assertThat(jdbc.queryForObject("SHOW statement_timeout", String.class)).isEqualTo("5s");
        assertThat(jdbc.queryForObject("SHOW idle_in_transaction_session_timeout", String.class)).isEqualTo("15s");
    }
}
