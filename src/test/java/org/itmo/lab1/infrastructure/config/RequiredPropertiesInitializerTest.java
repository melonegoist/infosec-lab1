package org.itmo.lab1.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.itmo.lab1.support.TestSecrets.randomBase64Key;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

class RequiredPropertiesInitializerTest {

    private final RequiredPropertiesInitializer initializer = new RequiredPropertiesInitializer();

    @Test
    void passesWhenEveryRequiredPropertyHasText() {
        MockEnvironment env = requiring("db.user", "db.password")
                .withProperty("db.user", "lab1_app")
                .withProperty("db.password", randomBase64Key(16));

        assertThatNoException().isThrownBy(() -> initialize(env));
    }

    @Test
    void resolvesPlaceholdersLikeTheRealConfiguration() {
        MockEnvironment env = requiring("db.user")
                .withProperty("db.user", "${DB_APP_USERNAME}")
                .withProperty("DB_APP_USERNAME", "lab1_app");

        assertThatNoException().isThrownBy(() -> initialize(env));
    }

    @Test
    void failsOnUnresolvedPlaceholderInsteadOfUsingItLiterally() {
        MockEnvironment env = requiring("db.user").withProperty("db.user", "${DB_APP_USERNAME}");

        assertThatIllegalStateException()
                .isThrownBy(() -> initialize(env))
                .withMessageContaining("[db.user]");
    }

    @Test
    void failsOnMissingAndBlankListingOnlyKeysNeverValues() {
        String presentSecret = randomBase64Key(16);
        MockEnvironment env = requiring("db.user", "db.password", "jwt.secret")
                .withProperty("db.user", presentSecret)
                .withProperty("db.password", "   ");

        assertThatIllegalStateException()
                .isThrownBy(() -> initialize(env))
                .withMessageContaining("[db.password, jwt.secret]")
                .withMessageNotContaining(presentSecret);
    }

    @Test
    void doesNothingWhenNoRequirementsDeclared() {
        assertThatNoException().isThrownBy(() -> initialize(new MockEnvironment()));
    }

    private void initialize(MockEnvironment env) {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.setEnvironment(env);
            initializer.initialize(context);
        }
    }

    private static MockEnvironment requiring(String... keys) {
        MockEnvironment env = new MockEnvironment();
        for (int i = 0; i < keys.length; i++) {
            env.setProperty(RequiredPropertiesInitializer.REQUIRED_PROPERTIES_KEY + "[" + i + "]", keys[i]);
        }
        return env;
    }
}
