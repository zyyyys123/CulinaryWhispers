package com.zyyyys.culinarywhispers.common.security;

import com.zyyyys.culinarywhispers.common.context.UserContext;
import com.zyyyys.culinarywhispers.common.utils.JwtUtil;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthContextFilterTest {

    @Test
    void doFilter_xUserId_isIgnoredAndUnauthorized() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UserService userService = mock(UserService.class);
        AuthContextFilter filter = new AuthContextFilter(jwtUtil, new ObjectMapper(), userService);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("X-User-Id", "123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(401, response.getStatus());
        assertNull(UserContext.getUserId());
    }

    @Test
    void doFilter_bearerToken_setsAndClearsContext() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UserService userService = mock(UserService.class);
        AuthContextFilter filter = new AuthContextFilter(jwtUtil, new ObjectMapper(), userService);

        Claims claims = mock(Claims.class);
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.parseToken("token")).thenReturn(claims);
        when(claims.get("userId")).thenReturn(7L);
        when(userService.getById(7L)).thenReturn(new User());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final Long[] userIdInsideChain = new Long[1];
        FilterChain chain = (req, res) -> userIdInsideChain[0] = UserContext.getUserId();

        filter.doFilter(request, response, chain);

        assertEquals(7L, userIdInsideChain[0]);
        assertNull(UserContext.getUserId());
    }

    @Test
    void doFilter_whitelist_allowsWithoutAuth() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UserService userService = mock(UserService.class);
        AuthContextFilter filter = new AuthContextFilter(jwtUtil, new ObjectMapper(), userService);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/recipe/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(), any());
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void doFilter_nonWhitelist_withoutAuth_throwsUnauthorized() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UserService userService = mock(UserService.class);
        AuthContextFilter filter = new AuthContextFilter(jwtUtil, new ObjectMapper(), userService);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertDoesNotThrow(() -> filter.doFilter(request, response, (req, res) -> {}));
        assertEquals(401, response.getStatus());
        assertNull(UserContext.getUserId());
    }
}
