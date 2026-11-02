package org.itmo.lab1.support;

import java.util.Map;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.MapPropertySource;

@Order(Ordered.HIGHEST_PRECEDENCE)
public final class WebSliceTestProperties implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("webSliceTestProperties", Map.of(
                "app.security.jwt.secret", TestSecrets.randomBase64Key(64),
                "spring.datasource.username", "not-used-in-web-slice",
                "spring.datasource.password", "not-used-in-web-slice",
                "spring.flyway.user", "not-used-in-web-slice",
                "spring.flyway.password", "not-used-in-web-slice")));
    }
}
