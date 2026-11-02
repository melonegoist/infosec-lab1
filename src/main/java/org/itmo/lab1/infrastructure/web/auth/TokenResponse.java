package org.itmo.lab1.infrastructure.web.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn) {

    static TokenResponse bearer(String accessToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, "Bearer", expiresInSeconds);
    }

    @Override
    public String toString() {
        return "TokenResponse[accessToken=<redacted>, tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}
