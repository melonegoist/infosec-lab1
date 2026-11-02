package org.itmo.lab1.infrastructure.security.jwt;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;

final class JwtFormat {

    static final JOSEObjectType ACCESS_TOKEN_TYPE = new JOSEObjectType("at+jwt");

    static final JWSAlgorithm ALGORITHM = JWSAlgorithm.HS256;

    static final int MAX_TOKEN_LENGTH = 2048;

    private JwtFormat() {
    }
}
