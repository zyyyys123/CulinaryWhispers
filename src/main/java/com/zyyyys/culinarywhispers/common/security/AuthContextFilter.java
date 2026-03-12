package com.zyyyys.culinarywhispers.common.security;

import com.zyyyys.culinarywhispers.common.context.UserContext;
import com.zyyyys.culinarywhispers.common.context.RequestContext;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.common.security.authz.AuthzProperties;
import com.zyyyys.culinarywhispers.common.security.authz.Role;
import com.zyyyys.culinarywhispers.common.utils.JwtUtil;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AuthContextFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final AuthzProperties authzProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            try {
                String requestId = RequestContext.ensureRequestId(request);
                String traceId = RequestContext.ensureTraceId(request);
                response.setHeader(RequestContext.HEADER_REQUEST_ID, requestId);
                response.setHeader(RequestContext.HEADER_TRACE_ID, traceId);

                String requestUri = request.getRequestURI();
                String method = request.getMethod();

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
                    long userId = Long.parseLong(String.valueOf(userIdObj));
                    if (userService.getById(userId) == null) {
                        throw new BusinessException(ResultCode.UNAUTHORIZED);
                    }
                    UserContext.setUserId(userId);
                    Object usernameObj = claims.get("username");
                    UserContext.setUsername(usernameObj == null ? null : String.valueOf(usernameObj));
                    UserContext.setRoles(resolveRoles(UserContext.getUsername(), authzProperties));
                    filterChain.doFilter(request, response);
                    return;
                }

                if (isWhitelisted(method, requestUri)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                throw new BusinessException(ResultCode.UNAUTHORIZED);
            } catch (BusinessException ex) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json;charset=UTF-8");
                Result<Void> result = Result.error(ex.getCode(), ex.getMessage());
                result.setRequestId(RequestContext.ensureRequestId(request));
                result.setTraceId(RequestContext.ensureTraceId(request));
                result.setTimestamp(System.currentTimeMillis());
                result.setPath(request.getRequestURI());
                response.getWriter().write(objectMapper.writeValueAsString(result));
            }
        } finally {
            UserContext.clear();
        }
    }

    private EnumSet<Role> resolveRoles(String username, AuthzProperties props) {
        EnumSet<Role> roles = EnumSet.of(Role.USER);
        if (username == null) {
            return roles;
        }
        if (containsIgnoreCase(props.getSuperadmins(), username)) {
            roles.add(Role.ADMIN);
            roles.add(Role.SUPERADMIN);
            return roles;
        }
        if (containsIgnoreCase(props.getAdmins(), username)) {
            roles.add(Role.ADMIN);
        }
        return roles;
    }

    private boolean containsIgnoreCase(List<String> list, String v) {
        if (list == null || list.isEmpty() || v == null) {
            return false;
        }
        for (String x : list) {
            if (x != null && x.equalsIgnoreCase(v)) {
                return true;
            }
        }
        return false;
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
        if ("GET".equalsIgnoreCase(method) && (path.startsWith("/api/user/profile/") || path.startsWith("/api/user/stats/"))) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/user/vip/plans")) {
            return true;
        }
        if (path.startsWith("/api/ai/chat")) {
            return true;
        }
        if (path.equals("/api/log/capture")) {
            return true;
        }
        if (path.startsWith("/api/search")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/uploads/")) {
            return true;
        }
        if (path.startsWith("/api/recipe/list")) {
            return true;
        }
        if (path.startsWith("/api/recipe/recommend")) {
            return true;
        }
        if (path.startsWith("/api/social/interact/status")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/social/comment/list")) {
            return true;
        }
        return "GET".equalsIgnoreCase(method) && path.startsWith("/api/recipe/");
    }
}
