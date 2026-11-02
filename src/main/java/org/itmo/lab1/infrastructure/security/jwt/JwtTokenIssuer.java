package org.itmo.lab1.infrastructure.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import org.itmo.lab1.application.auth.IssuedToken;
import org.itmo.lab1.application.port.out.TokenIssuer;
import org.itmo.lab1.domain.user.User;
import org.springframework.stereotype.Component;

@Component
public final class JwtTokenIssuer implements TokenIssuer {

    private final JwtProperties properties;
    private final Clock clock;
    private final JWSSigner signer;

    public JwtTokenIssuer(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        try {
            this.signer = new MACSigner(properties.secretBytes());
        } catch (KeyLengthException e) {
            throw new IllegalStateException("JWT signing key is shorter than 256 bits");
        }
    }

    @Override
    public IssuedToken issue(User user) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(properties.accessTokenTtl());
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.issuer())
                .audience(properties.audience())
                .subject(Long.toString(user.id().value()))
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .jwtID(UUID.randomUUID().toString())
                .build();
        JWSHeader header = new JWSHeader.Builder(JwtFormat.ALGORITHM).type(JwtFormat.ACCESS_TOKEN_TYPE).build();
        SignedJWT jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign access token", e);
        }
        return new IssuedToken(jwt.serialize(), expiresAt);
    }
}
