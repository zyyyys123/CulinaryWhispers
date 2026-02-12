package com.zyyyys.culinarywhispers.common.security.token;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "cw.token", name = "store", havingValue = "redis", matchIfMissing = true)
public class RedisTokenStore implements TokenStore {

    private static final String KEY_USER_TOKEN = "login:token:";
    private static final String KEY_TOKEN_USER = "login:jwt:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisTokenStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void save(Long userId, String token, Duration ttl) {
        String userKey = KEY_USER_TOKEN + userId;
        String tokenKey = KEY_TOKEN_USER + token;
        stringRedisTemplate.opsForValue().set(userKey, token, ttl);
        stringRedisTemplate.opsForValue().set(tokenKey, String.valueOf(userId), ttl);
    }

    @Override
    public Optional<Long> getUserIdByToken(String token) {
        String tokenKey = KEY_TOKEN_USER + token;
        String userId = stringRedisTemplate.opsForValue().get(tokenKey);
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(userId));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public void revoke(String token) {
        String tokenKey = KEY_TOKEN_USER + token;
        String userId = stringRedisTemplate.opsForValue().get(tokenKey);
        if (userId != null) {
            stringRedisTemplate.delete(KEY_USER_TOKEN + userId);
        }
        stringRedisTemplate.delete(tokenKey);
    }
}

