package com.zyyyys.culinarywhispers.common.security.token;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "cw.token", name = "store", havingValue = "memory")
public class InMemoryTokenStore implements TokenStore {

    private static final class Entry {
        private final Long userId;
        private final long expireAtMillis;

        private Entry(Long userId, long expireAtMillis) {
            this.userId = userId;
            this.expireAtMillis = expireAtMillis;
        }
    }

    private final ConcurrentHashMap<String, Entry> tokenToEntry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> userToToken = new ConcurrentHashMap<>();

    @Override
    public void save(Long userId, String token, Duration ttl) {
        long expireAt = System.currentTimeMillis() + ttl.toMillis();
        tokenToEntry.put(token, new Entry(userId, expireAt));
        userToToken.put(userId, token);
    }

    @Override
    public Optional<Long> getUserIdByToken(String token) {
        Entry entry = tokenToEntry.get(token);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expireAtMillis <= System.currentTimeMillis()) {
            tokenToEntry.remove(token);
            return Optional.empty();
        }
        return Optional.ofNullable(entry.userId);
    }

    @Override
    public void revoke(String token) {
        Entry entry = tokenToEntry.remove(token);
        if (entry != null) {
            userToToken.remove(entry.userId, token);
        }
    }
}

