package org.itmo.lab1.infrastructure.web.limit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

public final class RequestBodySizeLimitFilter extends OncePerRequestFilter {

    private final long maxBytes;
    private final HandlerExceptionResolver exceptionResolver;

    public RequestBodySizeLimitFilter(long maxBytes, HandlerExceptionResolver exceptionResolver) {
        this.maxBytes = maxBytes;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > maxBytes) {
            reject(request, response);
            return;
        }
        chain.doFilter(new BodyLimitedRequest(request, maxBytes), response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (exceptionResolver.resolveException(request, response, null, new RequestBodyTooLargeException()) == null
                && !response.isCommitted()) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        }
    }

    private static final class BodyLimitedRequest extends HttpServletRequestWrapper {

        private final long maxBytes;
        private ServletInputStream limitedStream;

        BodyLimitedRequest(HttpServletRequest request, long maxBytes) {
            super(request);
            this.maxBytes = maxBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (limitedStream == null) {
                limitedStream = new LimitedInputStream(super.getInputStream(), maxBytes);
            }
            return limitedStream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }

    private static final class LimitedInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long maxBytes;
        private long bytesRead;

        LimitedInputStream(ServletInputStream delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value != -1) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int allowed = (int) Math.min(length, maxBytes - bytesRead + 1);
            int read = delegate.read(buffer, offset, allowed);
            if (read > 0) {
                count(read);
            }
            return read;
        }

        private void count(int read) {
            bytesRead += read;
            if (bytesRead > maxBytes) {
                throw new RequestBodyTooLargeException();
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            delegate.setReadListener(listener);
        }
    }
}
