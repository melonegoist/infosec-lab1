package org.itmo.lab1.infrastructure.web.post;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.itmo.lab1.application.port.in.BrowsePostsUseCase;
import org.itmo.lab1.application.port.in.CreatePostUseCase;
import org.itmo.lab1.application.post.PageSize;
import org.itmo.lab1.application.post.PostPage;
import org.itmo.lab1.domain.post.Post;
import org.itmo.lab1.domain.post.PostContent;
import org.itmo.lab1.domain.post.PostId;
import org.itmo.lab1.domain.user.PasswordHash;
import org.itmo.lab1.domain.user.User;
import org.itmo.lab1.domain.user.UserId;
import org.itmo.lab1.domain.user.Username;
import org.itmo.lab1.infrastructure.security.SecurityConfig;
import org.itmo.lab1.infrastructure.security.jwt.JwtTokenIssuer;
import org.itmo.lab1.support.SecuredWebMvcTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SecuredWebMvcTest(controllers = PostController.class)
class PostControllerTest {

    private static final String XSS = "<script>alert(document.cookie)</script>";
    private static final String XSS_ENCODED = "&lt;script&gt;alert(document.cookie)&lt;/script&gt;";
    private static final Instant CREATED = Instant.parse("2026-09-30T12:00:00Z");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenIssuer issuer;

    @MockitoBean
    private CreatePostUseCase createPost;

    @MockitoBean
    private BrowsePostsUseCase browsePosts;

    @Test
    void createsPostAsTokenSubjectStoresRawAndReturnsEncoded() throws Exception {
        when(createPost.create(eq(new UserId(7)), any())).thenAnswer(call ->
                new Post(new PostId(1), new Username("alice"), call.getArgument(1), CREATED));

        createAs(7, "{\"content\":\"" + XSS + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("alice"))
                .andExpect(jsonPath("$.content").value(XSS_ENCODED))
                .andExpect(content().string(not(containsString("<script"))));
        verify(createPost).create(new UserId(7), new PostContent(XSS));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"content\":\"hi\",\"authorId\":1}",
            "{\"content\":\"hi\",\"author\":\"admin\"}",
            "{\"content\":\"hi\",\"id\":1}",
            "{\"content\":\"hi\",\"createdAt\":\"2000-01-01T00:00:00Z\"}"
    })
    void clientCannotSetAuthorOrServerControlledFields(String body) throws Exception {
        createAs(7, body).andExpect(status().isBadRequest());
        verifyNoInteractions(createPost);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "evil\u202Etxt", "nul\\u0000byte"})
    void invalidContentIs400WithoutEcho(String content) throws Exception {
        createAs(7, "{\"content\":\"" + content + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("content"));
        verifyNoInteractions(createPost);
    }

    @Test
    void oversizedContentIs400() throws Exception {
        createAs(7, "{\"content\":\"" + "a".repeat(PostContent.MAX_LENGTH + 1) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("content"));
        verifyNoInteractions(createPost);
    }

    @Test
    void anonymousCanNeitherPostNorRead() throws Exception {
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/data")).andExpect(status().isUnauthorized());
        verifyNoInteractions(createPost, browsePosts);
    }

    @Test
    void listsLatestWithDefaultLimitEncodingEveryItemWithSecurityHeaders() throws Exception {
        when(browsePosts.latest(PageSize.defaultSize())).thenReturn(new PostPage(List.of(
                postOf(12, "<img src=x onerror=alert(1)>"), postOf(11, "plain text")), new PostId(11)));

        readAs(7, "/api/data")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].content").value("&lt;img src=x onerror=alert(1)&gt;"))
                .andExpect(jsonPath("$.items[1].content").value("plain text"))
                .andExpect(jsonPath("$.nextCursor").value(11))
                .andExpect(content().string(not(containsString("<img"))))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", SecurityConfig.CONTENT_SECURITY_POLICY));
    }

    @Test
    void passesCursorAndLimitAndReportsLastPage() throws Exception {
        when(browsePosts.olderThan(new PostId(10), new PageSize(2)))
                .thenReturn(new PostPage(List.of(postOf(9, "last")), null));

        readAs(7, "/api/data?limit=2&before=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.nextCursor", nullValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"limit=0", "limit=51", "limit=1000000000", "limit=abc", "before=0", "before=-5",
            "before=abc", "before=1%20OR%201=1"})
    void invalidPagingParametersAre400(String query) throws Exception {
        readAs(7, "/api/data?" + query).andExpect(status().isBadRequest());
        verifyNoInteractions(browsePosts);
    }

    private ResultActions createAs(long userId, String json) throws Exception {
        return mvc.perform(post("/api/posts").header(HttpHeaders.AUTHORIZATION, bearer(userId))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions readAs(long userId, String uri) throws Exception {
        return mvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }

    private String bearer(long userId) {
        return "Bearer " + issuer.issue(new User(new UserId(userId), new Username("alice"),
                new PasswordHash("{argon2}$argon2id$x"))).value();
    }

    private static Post postOf(long id, String content) {
        return new Post(new PostId(id), new Username("alice"), new PostContent(content), CREATED);
    }
}
