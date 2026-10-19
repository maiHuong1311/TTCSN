package com.learnflow.backend;

import static org.junit.jupiter.api.Assertions.*;

import com.learnflow.backend.auth.PasswordResetOtpStore;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Explicit opt-in: never uses real email or production OTP keys. */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_INTEGRATION", matches = "true")
class RedisOtpIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    StringRedisTemplate redis;

    private PasswordResetOtpStore store;
    private final String email = "redis-check-" + UUID.randomUUID() + "@example.com";

    @BeforeEach
    void setup() {
        store = new PasswordResetOtpStore(redis, "forme:test:" + UUID.randomUUID() + ":");
    }

    @AfterEach
    void cleanup() {
        redis.delete(
            List.of(
                store.key(email, "otp"),
                store.key(email, "grant"),
                store.key(email, "cooldown"),
                store.key(email, "daily")
            )
        );
    }

    @Test
    void expiresOtpAndLocksAfterFiveWrongAttempts() {
        assertEquals(1, store.issue(email, "correct-hmac", 1, 0, "challenge"));
        assertEquals(0, store.issue(email, "new-hmac", 1, 0, "new"));
        assertTrue(store.ready(email, "challenge"));
        assertTrue(redis.getExpire(store.key(email, "otp")) > 0);
        assertTrue(redis.getExpire(store.key(email, "otp")) <= 600);
        for (int i = 0; i < 5; i++) assertFalse(store.verify(email, "wrong-hmac", "token"));
        assertFalse(store.verify(email, "correct-hmac", "token"));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(store.key(email, "otp"))));
    }

    @Test
    void verificationCreatesShortLivedGrantThatOnlyOneRequestCanConsume() throws Exception {
        store.issue(email, "hmac", 23, 4, "challenge");
        store.ready(email, "challenge");
        assertTrue(store.verify(email, "hmac", "token-hash"));
        assertFalse(store.verify(email, "hmac", "token-hash"));
        assertTrue(redis.getExpire(store.key(email, "grant")) <= 300);
        assertNull(store.consume(email, "wrong-token"));
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<PasswordResetOtpStore.Grant> consume = () -> store.consume(email, "token-hash");
            var results = executor.invokeAll(List.of(consume, consume));
            var grants = new ArrayList<PasswordResetOtpStore.Grant>();
            for (var r : results) {
                var grant = r.get(10, TimeUnit.SECONDS);
                if (grant != null) grants.add(grant);
            }
            assertEquals(List.of(new PasswordResetOtpStore.Grant(23, 4)), grants);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void resendInvalidatesPreviousCodeAndEnforcesDailyLimit() {
        store.issue(email, "old", 1, 0, "old-challenge");
        store.ready(email, "old-challenge");
        redis.delete(store.key(email, "cooldown"));
        assertEquals(1, store.issue(email, "new", 1, 0, "new-challenge"));
        store.ready(email, "new-challenge");
        assertFalse(store.verify(email, "old", "token"));
        redis.delete(store.key(email, "cooldown"));
        redis.opsForValue().set(store.key(email, "daily"), "10", Duration.ofDays(1));
        assertEquals(0, store.issue(email, "another", 1, 0, "another-challenge"));
        redis.expire(store.key(email, "otp"), Duration.ZERO);
        assertFalse(store.verify(email, "new", "token"));
    }
}
