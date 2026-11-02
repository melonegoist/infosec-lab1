package org.itmo.lab1.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap-user")
public record BootstrapUserProperties(String username, String password) {

    public BootstrapUserProperties {
        if (hasText(username) != hasText(password)) {
            throw new IllegalArgumentException("Invalid app.bootstrap-user configuration: set both "
                    + "APP_BOOTSTRAP_USERNAME and APP_BOOTSTRAP_PASSWORD, or neither");
        }
    }

    public boolean enabled() {
        return hasText(username);
    }

    @Override
    public String toString() {
        return "BootstrapUserProperties[username=" + username + ", password=<redacted>]";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
