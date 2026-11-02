package org.itmo.lab1.infrastructure.web.limit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration(proxyBeanMethods = false)
public class HttpLimitsConfig {

    @Bean
    FilterRegistrationBean<RequestBodySizeLimitFilter> requestBodySizeLimitFilter(
            HttpLimitsProperties properties,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        FilterRegistrationBean<RequestBodySizeLimitFilter> registration = new FilterRegistrationBean<>(
                new RequestBodySizeLimitFilter(properties.maxRequestBodySize().toBytes(), exceptionResolver));
        registration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER + 1);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
