package org.itmo.lab1.infrastructure.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.itmo.lab1.support.AttackerTokens.ACCESS_TOKEN_TYPE;
import static org.itmo.lab1.support.AttackerTokens.hmacSigned;
import static org.itmo.lab1.support.AttackerTokens.validClaims;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.User;
import org.itmo.lab1.domain.user.UserId;
import org.itmo.lab1.domain.user.Username;
import org.itmo.lab1.support.AttackerTokens;
import org.itmo.lab1.support.TestSecrets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JwtTokenVerifierTest {

    private static final Instant NOW = Instant.parse("2026-09-30T12:00:00Z");
    private static final User ALICE =
            new User(new UserId(42), new Username("alice"), new PasswordHash("{argon2}$argon2id$x"));

    private final String base64Key = TestSecrets.randomBase64Key(64);
    private final byte[] key = Base64.getDecoder().decode(base64Key);
    private final JwtProperties properties =
            new JwtProperties(base64Key, "lab1-auth", "lab1-api", Duration.ofMinutes(15), Duration.ofSeconds(30));
    private final JwtTokenIssuer issuer = new JwtTokenIssuer(properties, Clock.fixed(NOW, ZoneOffset.UTC));
    private final JwtTokenVerifier verifier = verifierAt(NOW);

    @Test
    void issuedTokenVerifiesToItsSubject() {
        assertThat(verifier.verify(issuer.issue(ALICE).value()).id()).isEqualTo(new UserId(42));
    }

    @Test
    void tokenIsExplicitlyTypedAndCarriesOnlyTheMinimum() throws Exception {
        SignedJWT jwt = SignedJWT.parse(issuer.issue(ALICE).value());
        JWTClaimsSet claims = jwt.getJWTClaimsSet();

        assertThat(jwt.getHeader().toJSONObject()).containsOnlyKeys("alg", "typ");
        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
        assertThat(jwt.getHeader().getType()).isEqualTo(ACCESS_TOKEN_TYPE);
        assertThat(claims.getClaims()).containsOnlyKeys("iss", "aud", "sub", "iat", "nbf", "exp", "jti");
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(Duration.between(claims.getIssueTime().toInstant(), claims.getExpirationTime().toInstant()))
                .isEqualTo(Duration.ofMinutes(15));
        assertThat(jwt.getPayload().toString()).doesNotContain("alice").doesNotContain("argon2");
    }

    @Test
    void everyTokenHasUniqueJti() {
        assertThat(IntStream.range(0, 20)
                .mapToObj(i -> jti(issuer.issue(ALICE).value()))
                .collect(Collectors.toSet())).hasSize(20);
    }

    @Test
    void acceptsTokenWithinClockSkewAfterExpiry() {
        String token = issuer.issue(ALICE).value();

        assertThat(verifierAt(NOW.plus(Duration.ofMinutes(15)).plusSeconds(29)).verify(token).id())
                .isEqualTo(new UserId(42));
    }

    @Test
    void rejectsTokenExpiredBeyondClockSkew() {
        String token = issuer.issue(ALICE).value();

        assertRejected(verifierAt(NOW.plus(Duration.ofMinutes(15)).plusSeconds(31)), token);
    }

    @Test
    void rejectsTokenNotYetValid() {
        JWTClaimsSet claims = validClaims(NOW).notBeforeTime(Date.from(NOW.plusSeconds(120))).build();

        assertRejected(hmacSigned(JWSAlgorithm.HS256, key, ACCESS_TOKEN_TYPE, claims));
    }

    @Test
    void rejectsUnsignedAlgNone() {
        assertRejected(AttackerTokens.unsecured(validClaims(NOW).build()));
    }

    @Test
    void rejectsAlgNoneWithStrippedSignatureOnRealToken() {
        String[] real = issuer.issue(ALICE).value().split("\\.");
        String noneHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"at+jwt\"}".getBytes());

        assertRejected(noneHeader + "." + real[1] + ".");
    }

    @Test
    void rejectsTamperedPayload() {
        String alicesToken = issuer.issue(ALICE).value();
        String adminPayloadDonor = issuer.issue(new User(new UserId(1), new Username("admin"),
                new PasswordHash("{argon2}$argon2id$y"))).value();

        assertRejected(AttackerTokens.withPayloadOf(alicesToken, adminPayloadDonor));
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        byte[] attackerKey = Base64.getDecoder().decode(TestSecrets.randomBase64Key(64));

        assertRejected(hmacSigned(JWSAlgorithm.HS256, attackerKey, ACCESS_TOKEN_TYPE, validClaims(NOW).build()));
    }

    @Test
    void rejectsOtherHmacAlgorithmEvenWithTheCorrectKey() {
        assertRejected(hmacSigned(JWSAlgorithm.HS512, key, ACCESS_TOKEN_TYPE, validClaims(NOW).build()));
    }

    @Test
    void rejectsAsymmetricAlgorithm() {
        assertRejected(AttackerTokens.rsaSigned(validClaims(NOW).build()));
    }

    @Test
    void rejectsUnknownCriticalHeader() {
        assertRejected(AttackerTokens.withUnknownCriticalHeader(key, validClaims(NOW).build()));
    }

    @Test
    void rejectsWrongOrMissingType() {
        assertRejected(hmacSigned(JWSAlgorithm.HS256, key, JOSEObjectType.JWT, validClaims(NOW).build()));
        assertRejected(hmacSigned(JWSAlgorithm.HS256, key, null, validClaims(NOW).build()));
    }

    @Test
    void rejectsForeignIssuerOrAudience() {
        assertRejected(signed(validClaims(NOW).issuer("evil-auth").build()));
        assertRejected(signed(validClaims(NOW).audience("other-api").build()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jti", "exp", "sub", "iat", "nbf", "iss", "aud"})
    void rejectsMissingRequiredClaim(String claim) {
        assertRejected(signed(validClaims(NOW).claim(claim, null).build()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"alice", "0", "-1", "99999999999999999999999"})
    void rejectsSubjectThatIsNotAPositiveUserId(String subject) {
        assertRejected(signed(validClaims(NOW).subject(subject).build()));
    }

    @Test
    void rejectsGarbageAndOversizedInput() {
        assertRejected("");
        assertRejected("abc");
        assertRejected("a.b.c");
        assertRejected("....");
        assertRejected("x".repeat(3000));
    }

    @Test
    void rejectionNeverEchoesTokenContent() {
        String forged = AttackerTokens.unsecured(validClaims(NOW).subject("1").build());

        assertThatThrownBy(() -> verifier.verify(forged))
                .isInstanceOf(InvalidBearerTokenException.class)
                .hasMessage("invalid bearer token")
                .hasNoCause();
    }

    private String signed(JWTClaimsSet claims) {
        return hmacSigned(JWSAlgorithm.HS256, key, ACCESS_TOKEN_TYPE, claims);
    }

    private void assertRejected(String token) {
        assertRejected(verifier, token);
    }

    private static void assertRejected(JwtTokenVerifier verifier, String token) {
        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(InvalidBearerTokenException.class);
    }

    private JwtTokenVerifier verifierAt(Instant now) {
        return new JwtTokenVerifier(properties, Clock.fixed(now, ZoneOffset.UTC));
    }

    private static String jti(String token) {
        try {
            return SignedJWT.parse(token).getJWTClaimsSet().getJWTID();
        } catch (java.text.ParseException e) {
            throw new IllegalStateException(e);
        }
    }
}
