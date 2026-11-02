package org.itmo.lab1.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.itmo.lab1.support.SecuredWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SecuredWebMvcTest(controllers = SecurityBaselineTest.ProbeController.class)
@Import(SecurityBaselineTest.ProbeController.class)
class SecurityBaselineTest {

    private static final String INTERNAL_DETAIL = "internal detail: jdbc:postgresql://db/lab1";
    private static final String REFLECTED_MARKER = "x-reflected-marker";

    @RestController
    static class ProbeController {

        @GetMapping("/api/probe")
        Map<String, String> read() {
            return Map.of("status", "ok");
        }

        @PostMapping(value = "/api/probe", consumes = MediaType.APPLICATION_JSON_VALUE)
        Map<String, String> write(@RequestBody Map<String, String> body) {
            return Map.of("status", "ok");
        }

        @GetMapping("/api/boom")
        void boom() {
            throw new IllegalStateException(INTERNAL_DETAIL);
        }

        @PostMapping("/auth/login")
        Map<String, String> login() {
            return Map.of("status", "public");
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationContext context;

    @Test
    void anonymousRequestToProtectedEndpointGets401ProblemWithBearerChallenge() throws Exception {
        mvc.perform(get("/api/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").doesNotExist());
    }

    @Test
    @WithMockUser
    void authenticatedRequestPassesWithSecurityHeaders() throws Exception {
        expectSecurityHeaders(mvc.perform(get("/api/probe")).andExpect(status().isOk()));
    }

    @Test
    void errorResponsesCarrySecurityHeadersToo() throws Exception {
        expectSecurityHeaders(mvc.perform(get("/api/probe")).andExpect(status().isUnauthorized()));
    }

    @Test
    void unknownPathsAreDeniedByDefaultForAnonymous() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void unknownPathsOutsideApiAreForbiddenEvenForAuthenticatedUsers() throws Exception {
        mvc.perform(get("/actuator/heapdump"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void onlyPostOnLoginIsPublic() throws Exception {
        mvc.perform(post("/auth/login")).andExpect(status().isOk());
        mvc.perform(get("/auth/login")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void csrfTokenIsNotRequiredBecauseNoAmbientCredentialsExist() throws Exception {
        mvc.perform(post("/api/probe").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void noHttpSessionOrCookieIsEverCreated() throws Exception {
        for (MvcResult result : new MvcResult[] {
                mvc.perform(get("/api/probe")).andReturn(),
                mvc.perform(post("/auth/login")).andReturn()}) {
            assertThat(result.getRequest().getSession(false)).isNull();
            assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        }
    }

    @Test
    @WithMockUser
    void unexpectedErrorsRevealNothing() throws Exception {
        mvc.perform(get("/api/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").doesNotExist())
                .andExpect(content().string(not(containsString("jdbc"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    @WithMockUser
    void frameworkErrorDetailsDoNotReflectClientInput() throws Exception {
        mvc.perform(post("/api/probe").contentType("application/" + REFLECTED_MARKER).content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().string(not(containsString(REFLECTED_MARKER))));
    }

    @Test
    void springBootDefaultUserWithGeneratedPasswordIsNotCreated() {
        assertThat(context.getBeanProvider(UserDetailsService.class).getIfAvailable()).isNull();
    }

    private static void expectSecurityHeaders(ResultActions actions) throws Exception {
        actions.andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", SecurityConfig.CONTENT_SECURITY_POLICY))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string("X-XSS-Protection", "0"));
    }
}
