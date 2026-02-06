package com.zyyyys.culinarywhispers.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.time.Duration;

/**
 * JWT工具类
 * @author zyyyys
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final Key KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION = 86400000; // 24 hours
    private static final String KEY_USER_TOKEN = "login:token:";
    private static final String KEY_TOKEN_USER = "login:jwt:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成Token
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        String token = createToken(claims);
        String userKey = KEY_USER_TOKEN + userId;
        String tokenKey = KEY_TOKEN_USER + token;
        stringRedisTemplate.opsForValue().set(userKey, token, Duration.ofMillis(EXPIRATION));
        stringRedisTemplate.opsForValue().set(tokenKey, String.valueOf(userId), Duration.ofMillis(EXPIRATION));
        return token;
    }

    /**
     * 创建Token
     */
    private String createToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析Token
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证Token是否过期
     */
    public boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            String tokenKey = KEY_TOKEN_USER + token;
            Boolean exists = stringRedisTemplate.hasKey(tokenKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            return false;
        }
    }

    public void revokeToken(String token) {
        String tokenKey = KEY_TOKEN_USER + token;
        String userId = stringRedisTemplate.opsForValue().get(tokenKey);
        if (userId != null) {
            stringRedisTemplate.delete(KEY_USER_TOKEN + userId);
        }
        stringRedisTemplate.delete(tokenKey);
    }
}
