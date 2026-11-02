package org.itmo.lab1.infrastructure.web.limit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.itmo.lab1.application.port.in.BrowsePostsUseCase;
import org.itmo.lab1.application.port.in.CreatePostUseCase;
import org.itmo.lab1.application.port.in.LoginUseCase;
import org.itmo.lab1.domain.post.Post;
import org.itmo.lab1.domain.post.PostContent;
import org.itmo.lab1.domain.post.PostId;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.User;
import org.itmo.lab1.domain.user.UserId;
import org.itmo.lab1.domain.user.Username;
import org.itmo.lab1.infrastructure.security.jwt.JwtTokenIssuer;
import org.itmo.lab1.infrastructure.web.auth.AuthController;
import org.itmo.lab1.infrastructure.web.post.PostController;
import org.itmo.lab1.support.SecuredWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SecuredWebMvcTest(controllers = {AuthController.class, PostController.class})
class RequestBodyLimitWebTest {

    private static final String OVERSIZED_JSON = "{\"content\":\"" + "a".repeat(20 * 1024) + "\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenIssuer issuer;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private CreatePostUseCase createPost;

    @MockitoBean
    private BrowsePostsUseCase browsePosts;

    @Test
    void oversizedPostIsRejectedWith413BeforeReachingTheUseCase() throws Exception {
        mvc.perform(post("/api/posts").header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(OVERSIZED_JSON))
                .andExpect(status().isContentTooLarge())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        verifyNoInteractions(createPost);
    }

    @Test
    void oversizedLoginNeverTriggersPasswordHashing() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"" + "p".repeat(20 * 1024) + "\"}"))
                .andExpect(status().isContentTooLarge());
        verifyNoInteractions(loginUseCase);
    }

    @Test
    void anonymousOversizedRequestIsRejectedByAuthenticationFirst() throws Exception {
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(OVERSIZED_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void largestLegitimatePostFitsTheLimit() throws Exception {
        String maxContent = "\uD83D\uDE00".repeat(PostContent.MAX_LENGTH);
        when(createPost.create(any(), any())).thenReturn(new Post(new PostId(1), new Username("alice"),
                new PostContent(maxContent), Instant.parse("2026-09-30T12:00:00Z")));

        mvc.perform(post("/api/posts").header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"" + maxContent + "\"}"))
                .andExpect(status().isCreated());
    }

    private String bearer() {
        return "Bearer " + issuer.issue(new User(new UserId(7), new Username("alice"),
                new PasswordHash("{argon2}$argon2id$x"))).value();
    }
}
