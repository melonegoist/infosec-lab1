package org.itmo.lab1.infrastructure.web.limit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

class RequestBodySizeLimitFilterTest {

    private static final int LIMIT = 16;

    private final List<Exception> resolved = new ArrayList<>();
    private final HandlerExceptionResolver resolver = (request, response, handler, exception) -> {
        resolved.add(exception);
        response.setStatus(413);
        return new ModelAndView();
    };
    private final RequestBodySizeLimitFilter filter = new RequestBodySizeLimitFilter(LIMIT, resolver);

    @Test
    void declaredOversizedBodyIsRejectedBeforeAnyoneReadsIt() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWithBody(LIMIT + 1), response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(resolved).singleElement().isInstanceOf(RequestBodyTooLargeException.class);
    }

    @Test
    void bodyWithinLimitIsPassedThroughIntact() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWithBody(LIMIT), new MockHttpServletResponse(), chain);

        byte[] body = ((HttpServletRequest) chain.getRequest()).getInputStream().readAllBytes();
        assertThat(body).hasSize(LIMIT);
    }

    @Test
    void undeclaredLengthIsCutOffWhileStreaming() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(chunkedRequestWithBody(LIMIT + 1), new MockHttpServletResponse(), chain);

        HttpServletRequest passed = (HttpServletRequest) chain.getRequest();
        assertThatThrownBy(() -> passed.getInputStream().readAllBytes())
                .isInstanceOf(RequestBodyTooLargeException.class);
    }

    @Test
    void undeclaredLengthAtExactlyTheLimitIsAccepted() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(chunkedRequestWithBody(LIMIT), new MockHttpServletResponse(), chain);

        assertThat(((HttpServletRequest) chain.getRequest()).getInputStream().readAllBytes()).hasSize(LIMIT);
    }

    @Test
    void readerIsLimitedAsWellAsInputStream() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(chunkedRequestWithBody(LIMIT + 1), new MockHttpServletResponse(), chain);

        HttpServletRequest passed = (HttpServletRequest) chain.getRequest();
        assertThatThrownBy(() -> passed.getReader().transferTo(java.io.Writer.nullWriter()))
                .isInstanceOf(RequestBodyTooLargeException.class);
    }

    @Test
    void failsClosedWithBare413WhenResolverCannotHandleIt() throws Exception {
        RequestBodySizeLimitFilter withoutResolver =
                new RequestBodySizeLimitFilter(LIMIT, (request, response, handler, exception) -> null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        withoutResolver.doFilter(requestWithBody(LIMIT + 1), response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(413);
    }

    private static MockHttpServletRequest requestWithBody(int size) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/posts");
        request.setContent("x".repeat(size).getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private static MockHttpServletRequest chunkedRequestWithBody(int size) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/posts") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }

            @Override
            public int getContentLength() {
                return -1;
            }
        };
        request.setContent("x".repeat(size).getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
