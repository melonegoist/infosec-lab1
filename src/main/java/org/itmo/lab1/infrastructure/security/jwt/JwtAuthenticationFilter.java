package org.itmo.lab1.infrastructure.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.itmo.lab1.infrastructure.security.AuthenticatedUser;
import org.itmo.lab1.infrastructure.security.AuthenticatedUserToken;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

public final class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final Pattern B64TOKEN = Pattern.compile("[A-Za-z0-9\\-._~+/]+=*");

    private final JwtTokenVerifier verifier;
    private final AuthenticationEntryPoint entryPoint;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository contextRepository = new RequestAttributeSecurityContextRepository();

    public JwtAuthenticationFilter(JwtTokenVerifier verifier, AuthenticationEntryPoint entryPoint) {
        this.verifier = verifier;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        List<String> authorization = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));
        if (authorization.isEmpty() || !hasBearerScheme(authorization)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            AuthenticatedUser user = verifier.verify(extractToken(authorization));
            SecurityContext context = contextHolder.createEmptyContext();
            context.setAuthentication(new AuthenticatedUserToken(user));
            contextHolder.setContext(context);
            contextRepository.saveContext(context, request, response);
        } catch (InvalidBearerTokenException e) {
            contextHolder.clearContext();
            entryPoint.commence(request, response, e);
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean hasBearerScheme(List<String> authorization) {
        return authorization.stream().anyMatch(h -> h.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length()));
    }

    private static String extractToken(List<String> authorization) {
        if (authorization.size() != 1) {
            throw new InvalidBearerTokenException();
        }
        String token = authorization.getFirst().substring(BEARER_PREFIX.length());
        if (token.length() > JwtFormat.MAX_TOKEN_LENGTH || !B64TOKEN.matcher(token).matches()) {
            throw new InvalidBearerTokenException();
        }
        return token;
    }
}
