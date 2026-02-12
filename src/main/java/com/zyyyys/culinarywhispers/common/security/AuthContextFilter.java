package com.zyyyys.culinarywhispers.common.security;

import com.zyyyys.culinarywhispers.common.context.UserContext;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AuthContextFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String requestUri = request.getRequestURI();
            String method = request.getMethod();

            String headerUserId = request.getHeader(HEADER_USER_ID);
            if (headerUserId != null && !headerUserId.isBlank()) {
                try {
                    UserContext.setUserId(Long.parseLong(headerUserId.trim()));
                } catch (NumberFormatException ignored) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED);
                }
                filterChain.doFilter(request, response);
                return;
            }

            String authHeader = request.getHeader(HEADER_AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length()).trim();
                if (!jwtUtil.validateToken(token)) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED);
                }
                Claims claims = jwtUtil.parseToken(token);
                Object userIdObj = claims.get("userId");
                if (userIdObj == null) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED);
                }
                UserContext.setUserId(Long.parseLong(String.valueOf(userIdObj)));
                filterChain.doFilter(request, response);
                return;
            }

            if (isWhitelisted(method, requestUri)) {
                filterChain.doFilter(request, response);
                return;
            }

            throw new BusinessException(ResultCode.UNAUTHORIZED);
        } finally {
            UserContext.clear();
        }
    }

    private boolean isWhitelisted(String method, String path) {
        if (path == null) {
            return true;
        }
        if (path.startsWith("/actuator") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return true;
        }
        if (path.equals("/api/user/login") || path.equals("/api/user/register")) {
            return true;
        }
        if (path.startsWith("/api/search")) {
            return true;
        }
        if (path.startsWith("/api/recipe/list")) {
            return true;
        }
        return "GET".equalsIgnoreCase(method) && path.startsWith("/api/recipe/");
    }
}
