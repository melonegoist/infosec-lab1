package org.itmo.lab1.infrastructure.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JWSAlgorithm;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.User;
import org.itmo.lab1.domain.user.UserId;
import org.itmo.lab1.domain.user.Username;
import org.itmo.lab1.infrastructure.security.AuthenticatedUser;
import org.itmo.lab1.support.AttackerTokens;
import org.itmo.lab1.support.SecuredWebMvcTest;
import org.itmo.lab1.support.TestSecrets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SecuredWebMvcTest(controllers = JwtAuthenticationWebTest.ProbeController.class)
@Import(JwtAuthenticationWebTest.ProbeController.class)
class JwtAuthenticationWebTest {

    private static final String INVALID_TOKEN_CHALLENGE = "Bearer error=\"invalid_token\"";

    @RestController
    static class ProbeController {

        @GetMapping("/api/whoami")
        Map<String, Long> whoami(@AuthenticationPrincipal AuthenticatedUser user) {
            return Map.of("userId", user.id().value());
        }

        @PostMapping("/auth/login")
        Map<String, String> login() {
            return Map.of("status", "public");
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenIssuer issuer;

    private String validToken() {
        return issuer.issue(new User(new UserId(42), new Username("alice"), new PasswordHash("{argon2}$argon2id$x")))
                .value();
    }

    private static String forgedToken() {
        byte[] attackerKey = Base64.getDecoder().decode(TestSecrets.randomBase64Key(32));
        return AttackerTokens.hmacSigned(JWSAlgorithm.HS256, attackerKey, AttackerTokens.ACCESS_TOKEN_TYPE,
                AttackerTokens.validClaims(Instant.now()).build());
    }

    @Test
    void validTokenAuthenticatesRequestAsTokenSubject() throws Exception {
        mvc.perform(get("/api/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42));
    }

    @Test
    void schemeNameIsCaseInsensitive() throws Exception {
        mvc.perform(get("/api/whoami").header(HttpHeaders.AUTHORIZATION, "bearer " + validToken()))
                .andExpect(status().isOk());
    }

    @Test
    void missingTokenGetsPlainBearerChallenge() throws Exception {
        mvc.perform(get("/api/whoami"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @Test
    void forgedTokenGetsInvalidTokenChallengeWithoutDetails() throws Exception {
        mvc.perform(get("/api/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, INVALID_TOKEN_CHALLENGE))
                .andExpect(jsonPath("$.detail").doesNotExist());
    }

    @Test
    void tokenInQueryParameterIsNotAccepted() throws Exception {
        mvc.perform(get("/api/whoami").param("access_token", validToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @Test
    void otherAuthenticationSchemesAreNotAccepted() throws Exception {
        mvc.perform(get("/api/whoami").header(HttpHeaders.AUTHORIZATION, "Basic YWxpY2U6cGFzc3dvcmQ="))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer ", "Bearer  token", "Bearer a b", "Bearer <script>", "Bearer abc\"def"})
    void malformedBearerCredentialsAreRejected(String authorization) throws Exception {
        mvc.perform(get("/api/whoami").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, INVALID_TOKEN_CHALLENGE));
    }

    @Test
    void multipleAuthorizationHeadersAreRejectedEvenIfOneIsValid() throws Exception {
        mvc.perform(get("/api/whoami")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken(), "Bearer " + forgedToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, INVALID_TOKEN_CHALLENGE));
    }

    @Test
    void presentedInvalidTokenIsRejectedEvenOnPublicEndpoint() throws Exception {
        mvc.perform(post("/auth/login").header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login")).andExpect(status().isOk());
    }

    @Test
    void authenticatedRequestsStayStateless() throws Exception {
        MvcResult result = mvc.perform(get("/api/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }
}
