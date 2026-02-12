package com.zyyyys.culinarywhispers.common.utils;

import com.zyyyys.culinarywhispers.common.security.token.InMemoryTokenStore;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void generate_validate_parse_revoke() {
        JwtUtil jwtUtil = new JwtUtil(new InMemoryTokenStore());
        ReflectionTestUtils.setField(jwtUtil, "secret", "mySecretKeyForCulinaryWhispersProject2026");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60_000L);
        jwtUtil.init();

        String token = jwtUtil.generateToken(1L, "u1");
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));

        Claims claims = jwtUtil.parseToken(token);
        assertEquals("u1", claims.get("username"));
        assertEquals(1, Integer.parseInt(String.valueOf(claims.get("userId"))));

        jwtUtil.revokeToken(token);
        assertFalse(jwtUtil.validateToken(token));
    }
}

