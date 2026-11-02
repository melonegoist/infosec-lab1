package org.itmo.lab1.infrastructure.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.text.ParseException;
import java.time.Clock;
import java.util.Date;
import java.util.Set;
import org.itmo.lab1.domain.DomainValidationException;
import org.itmo.lab1.domain.user.UserId;
import org.itmo.lab1.infrastructure.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenVerifier.class);

    private static final Set<String> REQUIRED_CLAIMS = Set.of(
            JWTClaimNames.ISSUER, JWTClaimNames.AUDIENCE, JWTClaimNames.SUBJECT, JWTClaimNames.ISSUED_AT,
            JWTClaimNames.NOT_BEFORE, JWTClaimNames.EXPIRATION_TIME, JWTClaimNames.JWT_ID);

    private final ConfigurableJWTProcessor<SecurityContext> processor;

    public JwtTokenVerifier(JwtProperties properties, Clock clock) {
        DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(JwtFormat.ACCESS_TOKEN_TYPE));
        jwtProcessor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(JwtFormat.ALGORITHM, new ImmutableSecret<>(properties.secretBytes())));

        DefaultJWTClaimsVerifier<SecurityContext> claimsVerifier = new DefaultJWTClaimsVerifier<>(
                Set.of(properties.audience()),
                new JWTClaimsSet.Builder().issuer(properties.issuer()).build(),
                REQUIRED_CLAIMS,
                null) {
            @Override
            protected Date currentTime() {
                return Date.from(clock.instant());
            }
        };
        claimsVerifier.setMaxClockSkew(Math.toIntExact(properties.clockSkew().toSeconds()));
        jwtProcessor.setJWTClaimsSetVerifier(claimsVerifier);
        this.processor = jwtProcessor;
    }

    public AuthenticatedUser verify(String token) {
        if (token == null || token.isEmpty() || token.length() > JwtFormat.MAX_TOKEN_LENGTH) {
            throw rejected("length");
        }
        JWTClaimsSet claims;
        try {
            claims = processor.process(token, null);
        } catch (ParseException | BadJOSEException | JOSEException | RuntimeException e) {
            throw rejected(e.getClass().getSimpleName());
        }
        return new AuthenticatedUser(parseSubject(claims.getSubject()));
    }

    private static UserId parseSubject(String subject) {
        try {
            return new UserId(Long.parseLong(subject));
        } catch (NumberFormatException | DomainValidationException e) {
            throw rejected("subject");
        }
    }

    private static InvalidBearerTokenException rejected(String reason) {
        log.debug("Bearer token rejected: {}", reason);
        return new InvalidBearerTokenException();
    }
}
