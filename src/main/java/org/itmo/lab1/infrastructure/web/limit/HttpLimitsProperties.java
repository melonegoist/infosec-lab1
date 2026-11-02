package org.itmo.lab1.infrastructure.web.limit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.http")
public record HttpLimitsProperties(DataSize maxRequestBodySize) {

    private static final DataSize UPPER_BOUND = DataSize.ofMegabytes(1);

    public HttpLimitsProperties {
        if (maxRequestBodySize == null || maxRequestBodySize.toBytes() < 1
                || maxRequestBodySize.compareTo(UPPER_BOUND) > 0) {
            throw new IllegalArgumentException(
                    "Invalid app.http configuration: max-request-body-size must be in [1B, " + UPPER_BOUND + "]");
        }
    }
}
