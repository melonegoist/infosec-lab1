package org.itmo.lab1.infrastructure.web.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.itmo.lab1.application.auth.InvalidCredentialsException;
import org.itmo.lab1.application.auth.IssuedToken;
import org.itmo.lab1.application.auth.LoginCommand;
import org.itmo.lab1.application.port.in.LoginUseCase;
import org.itmo.lab1.application.port.out.PasswordHasherBusyException;
import org.itmo.lab1.domain.user.Username;
import org.itmo.lab1.support.SecuredWebMvcTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SecuredWebMvcTest(controllers = AuthController.class)
class AuthControllerTest {

    private static final String PASSWORD = "correct horse battery staple";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @Test
    void issuesBearerTokenForValidCredentials() throws Exception {
        when(loginUseCase.login(any())).thenReturn(new IssuedToken("h.p.s", Instant.now().plusSeconds(900)));

        login("{\"username\":\"Alice\",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.access_token").value("h.p.s"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in", allOf(greaterThanOrEqualTo(898), lessThanOrEqualTo(900))))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(content().string(not(containsString(PASSWORD))));

        ArgumentCaptor<LoginCommand> command = ArgumentCaptor.forClass(LoginCommand.class);
        verify(loginUseCase).login(command.capture());
        assertThat(command.getValue().username()).isEqualTo(new Username("alice"));
    }

    @Test
    void invalidCredentialsGetGeneric401() throws Exception {
        when(loginUseCase.login(any())).thenThrow(new InvalidCredentialsException());

        login("{\"username\":\"alice\",\"password\":\"wrong password\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").doesNotExist());
    }

    @Test
    void malformedUsernameIs400WithoutEchoingInputOrCallingUseCase() throws Exception {
        login("{\"username\":\"<script>alert(1)</script>\",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("username"))
                .andExpect(content().string(not(containsString("<script>"))));
        verifyNoInteractions(loginUseCase);
    }

    @Test
    void oversizedPasswordIs400() throws Exception {
        login("{\"username\":\"alice\",\"password\":\"" + "a".repeat(129) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("password"));
        verifyNoInteractions(loginUseCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"username\":\"alice\"}",
            "{\"username\":\"alice\",\"password\":\"" + PASSWORD + "\",\"role\":\"admin\"}",
            "{\"username\":\"alice\",\"password\":\"" + PASSWORD + "\"}{\"username\":\"admin\"}",
            "{\"username\":\"alice\",",
            "[\"alice\",\"" + PASSWORD + "\"]"
    })
    void structurallyInvalidBodiesAre400AndNeverReachUseCase(String body) throws Exception {
        login(body).andExpect(status().isBadRequest());
        verifyNoInteractions(loginUseCase);
    }

    @Test
    void credentialsAreAcceptedOnlyAsJsonBody() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("username=alice&password=x"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/auth/login").param("username", "alice").param("password", PASSWORD))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(loginUseCase);
    }

    @Test
    void saturatedPasswordHashingIs503WithRetryAfter() throws Exception {
        when(loginUseCase.login(any())).thenThrow(new PasswordHasherBusyException());

        login("{\"username\":\"alice\",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "1"));
    }

    private ResultActions login(String json) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
