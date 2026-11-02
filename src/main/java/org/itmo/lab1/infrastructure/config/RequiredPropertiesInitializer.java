package org.itmo.lab1.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public final class RequiredPropertiesInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String REQUIRED_PROPERTIES_KEY = "app.required-properties";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        Environment environment = context.getEnvironment();
        List<String> missing = Binder.get(environment)
                .bind(REQUIRED_PROPERTIES_KEY, Bindable.listOf(String.class))
                .orElseGet(List::of)
                .stream()
                .filter(key -> !hasResolvedText(environment, key))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Required configuration is missing, blank or unresolved: " + missing
                    + ". Provide the environment variables listed in .env.example.");
        }
    }

    private static boolean hasResolvedText(Environment environment, String key) {
        try {
            return StringUtils.hasText(environment.getProperty(key));
        } catch (IllegalArgumentException unresolvedPlaceholder) {
            return false;
        }
    }
}
