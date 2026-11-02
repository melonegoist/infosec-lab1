package org.itmo.lab1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.itmo.lab1.application.port.in.ProvisionUserUseCase;
import org.itmo.lab1.application.port.out.UserRepository;
import org.itmo.lab1.domain.user.RawPassword;
import org.itmo.lab1.domain.user.Username;
import org.itmo.lab1.infrastructure.security.jwt.JwtTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@Transactional
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "correct horse battery staple";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProvisionUserUseCase provisioning;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtTokenVerifier verifier;

    private final String username = "it_" + randomHex();

    @BeforeEach
    void createUser() {
        provisioning.ensureUserExists(new Username(username), new RawPassword(PASSWORD));
    }

    @Test
    void loginIssuesTokenBoundToTheUsersId() throws Exception {
        String body = login(username, PASSWORD).andReturnBody(200);

        String token = JsonPath.read(body, "$.access_token");
        assertThat(verifier.verify(token).id())
                .isEqualTo(users.findByUsername(new Username(username)).orElseThrow().id());
    }

    @Test
    void usernameIsCaseInsensitive() throws Exception {
        login(username.toUpperCase(Locale.ROOT), PASSWORD).andReturnBody(200);
    }

    @Test
    void wrongPasswordAndUnknownUserAreIndistinguishable() throws Exception {
        MvcResult wrongPassword = login(username, "wrong password, definitely").result(401);
        MvcResult unknownUser = login("nobody_" + randomHex(), PASSWORD).result(401);

        assertThat(unknownUser.getResponse().getContentAsString())
                .isEqualTo(wrongPassword.getResponse().getContentAsString());
        assertThat(unknownUser.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
                .isEqualTo(wrongPassword.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE));
    }

    private LoginCall login(String user, String password) {
        return new LoginCall(user, password);
    }

    private final class LoginCall {

        private final String json;

        LoginCall(String user, String password) {
            this.json = "{\"username\":\"" + user + "\",\"password\":\"" + password + "\"}";
        }

        MvcResult result(int expectedStatus) throws Exception {
            return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().is(expectedStatus))
                    .andReturn();
        }

        String andReturnBody(int expectedStatus) throws Exception {
            return result(expectedStatus).getResponse().getContentAsString();
        }
    }

    private static String randomHex() {
        byte[] bytes = new byte[4];
        ThreadLocalRandom.current().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
