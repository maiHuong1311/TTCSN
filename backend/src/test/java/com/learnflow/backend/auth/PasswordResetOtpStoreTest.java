package com.learnflow.backend.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.learnflow.backend.common.web.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;

class PasswordResetOtpStoreTest {

    @Test
    void redisKeysHideEmailAndShareAClusterSlot() {
        var store = new PasswordResetOtpStore(mock(StringRedisTemplate.class), "forme:test:");
        String otp = store.key("customer@example.com", "otp");
        assertFalse(otp.contains("customer@example.com"));
        assertTrue(otp.matches("forme:test:\\{[a-f0-9]{64}\\}:otp"));
        assertEquals(
            otp.substring(0, otp.lastIndexOf(':')),
            store.key("customer@example.com", "grant").substring(0, otp.lastIndexOf(':'))
        );
        assertNotEquals(otp, store.key("other@example.com", "otp"));
    }

    @Test
    void redisFailureReturnsVietnamese503WithoutInternalDetails() {
        var redis = mock(StringRedisTemplate.class);
        when(
            redis.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                any(Object[].class)
            )
        ).thenThrow(new RedisConnectionFailureException("private-connection-details"));
        var store = new PasswordResetOtpStore(redis, "forme:test:");
        var error = assertThrows(ApiException.class, () ->
            store.issue("customer@example.com", "hash", 1, 0, "challenge")
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.status());
        assertTrue(error.getMessage().contains("Dịch vụ xác minh"));
        assertFalse(error.getMessage().contains("private-connection-details"));
    }
}
