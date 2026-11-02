package org.itmo.lab1.infrastructure.bootstrap;

import org.itmo.lab1.application.port.in.ProvisionUserUseCase;
import org.itmo.lab1.domain.user.RawPassword;
import org.itmo.lab1.domain.user.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapUserRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapUserRunner.class);

    private final BootstrapUserProperties properties;
    private final ProvisionUserUseCase provisioning;

    public BootstrapUserRunner(BootstrapUserProperties properties, ProvisionUserUseCase provisioning) {
        this.properties = properties;
        this.provisioning = provisioning;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            log.info("Bootstrap user is not configured (APP_BOOTSTRAP_USERNAME), skipping");
            return;
        }
        Username username = Username.normalize(properties.username());
        boolean created = provisioning.ensureUserExists(username, RawPassword.normalize(properties.password()));
        log.info(created ? "Bootstrap user created: {}" : "Bootstrap user already exists, left unchanged: {}",
                username.value());
    }
}
