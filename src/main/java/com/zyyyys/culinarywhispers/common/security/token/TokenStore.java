package com.zyyyys.culinarywhispers.common.security.token;

import java.time.Duration;
import java.util.Optional;

public interface TokenStore {

    void save(Long userId, String token, Duration ttl);

    Optional<Long> getUserIdByToken(String token);

    void revoke(String token);
}

