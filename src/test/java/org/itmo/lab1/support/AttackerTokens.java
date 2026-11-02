package org.itmo.lab1.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public final class AttackerTokens {

    public static final JOSEObjectType ACCESS_TOKEN_TYPE = new JOSEObjectType("at+jwt");

    private AttackerTokens() {
    }

    public static JWTClaimsSet.Builder validClaims(Instant now) {
        return new JWTClaimsSet.Builder()
                .issuer("lab1-auth")
                .audience("lab1-api")
                .subject("42")
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(900)))
                .jwtID(UUID.randomUUID().toString());
    }

    public static String hmacSigned(JWSAlgorithm algorithm, byte[] key, JOSEObjectType type, JWTClaimsSet claims) {
        JWSHeader.Builder header = new JWSHeader.Builder(algorithm);
        if (type != null) {
            header.type(type);
        }
        return sign(new SignedJWT(header.build(), claims), key);
    }

    public static String withUnknownCriticalHeader(byte[] key, JWTClaimsSet claims) {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256).type(ACCESS_TOKEN_TYPE)
                .criticalParams(java.util.Set.of("x-attacker"))
                .customParam("x-attacker", true)
                .build();
        return sign(new SignedJWT(header, claims), key);
    }

    public static String unsecured(JWTClaimsSet claims) {
        return new PlainJWT(new PlainHeader.Builder().type(ACCESS_TOKEN_TYPE).build(), claims).serialize();
    }

    public static String rsaSigned(JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).type(ACCESS_TOKEN_TYPE).build(), claims);
            jwt.sign(new RSASSASigner(new RSAKeyGenerator(2048).generate()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String withPayloadOf(String signedToken, String donorToken) {
        String[] target = signedToken.split("\\.");
        String[] donor = donorToken.split("\\.");
        return target[0] + "." + donor[1] + "." + target[2];
    }

    private static String sign(SignedJWT jwt, byte[] key) {
        try {
            jwt.sign(new MACSigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
