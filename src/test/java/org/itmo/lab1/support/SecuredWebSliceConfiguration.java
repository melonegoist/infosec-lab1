package org.itmo.lab1.support;

import org.itmo.lab1.infrastructure.config.ClockConfig;
import org.itmo.lab1.infrastructure.security.SecurityConfig;
import org.itmo.lab1.infrastructure.security.SecurityProblemHandler;
import org.itmo.lab1.infrastructure.security.jwt.JwtProperties;
import org.itmo.lab1.infrastructure.security.jwt.JwtTokenIssuer;
import org.itmo.lab1.infrastructure.security.jwt.JwtTokenVerifier;
import org.itmo.lab1.infrastructure.web.limit.HttpLimitsConfig;
import org.itmo.lab1.infrastructure.web.limit.HttpLimitsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtProperties.class, HttpLimitsProperties.class})
@Import({SecurityConfig.class, SecurityProblemHandler.class, JwtTokenVerifier.class, JwtTokenIssuer.class,
        ClockConfig.class, HttpLimitsConfig.class})
class SecuredWebSliceConfiguration {
}
