package org.itmo.lab1.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.itmo.lab1.infrastructure.persistence.post.JpaPostRepositoryAdapter;
import org.itmo.lab1.infrastructure.persistence.user.JpaUserRepositoryAdapter;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DataJpaTest(properties =
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=org.itmo.lab1.support.SqlCapture")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles({"local", "test"})
@Import({JpaUserRepositoryAdapter.class, JpaPostRepositoryAdapter.class})
public @interface PostgresJpaTest {
}
