package org.itmo.lab1.support;

import java.security.SecureRandom;
import java.util.Base64;

public final class TestSecrets {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TestSecrets() {
    }

    public static String randomBase64Key(int bytes) {
        byte[] key = new byte[bytes];
        RANDOM.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
