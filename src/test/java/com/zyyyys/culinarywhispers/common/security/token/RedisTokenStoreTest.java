package com.zyyyys.culinarywhispers.common.security.token;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisTokenStoreTest {

    @Test
    void save_and_getUserId() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        RedisTokenStore store = new RedisTokenStore(redisTemplate);
        store.save(1L, "t", Duration.ofSeconds(10));

        verify(valueOps).set(eq("login:token:1"), eq("t"), any(Duration.class));
        verify(valueOps).set(eq("login:jwt:t"), eq("1"), any(Duration.class));

        when(valueOps.get("login:jwt:t")).thenReturn("12");
        Optional<Long> userId = store.getUserIdByToken("t");
        assertTrue(userId.isPresent());
        assertEquals(12L, userId.get());
    }

    @Test
    void revoke_deletesKeys() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("login:jwt:t")).thenReturn("9");

        RedisTokenStore store = new RedisTokenStore(redisTemplate);
        store.revoke("t");

        verify(redisTemplate).delete("login:token:9");
        verify(redisTemplate).delete("login:jwt:t");
    }
}

