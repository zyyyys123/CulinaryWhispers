package com.zyyyys.culinarywhispers.common.security.token;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTokenStoreTest {

    @Test
    void save_get_revoke() {
        InMemoryTokenStore store = new InMemoryTokenStore();
        store.save(1L, "t1", Duration.ofSeconds(60));
        assertEquals(1L, store.getUserIdByToken("t1").orElseThrow());

        store.revoke("t1");
        assertTrue(store.getUserIdByToken("t1").isEmpty());
    }

    @Test
    void expired_token_returnsEmpty() throws Exception {
        InMemoryTokenStore store = new InMemoryTokenStore();
        store.save(2L, "t2", Duration.ofMillis(1));
        Thread.sleep(2);
        assertTrue(store.getUserIdByToken("t2").isEmpty());
    }
}

