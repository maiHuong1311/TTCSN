package com.learnflow.backend.auth;

import com.learnflow.backend.common.web.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetOtpStore {

    private final StringRedisTemplate redis;
    private final String prefix;
    private static final DefaultRedisScript<Long> ISSUE = new DefaultRedisScript<>(
        """
        if redis.call('EXISTS',KEYS[2]) == 1 then return 0 end
        if tonumber(redis.call('GET',KEYS[3]) or '0') >= 10 then return 0 end
        if redis.call('HGET',KEYS[1],'code') == ARGV[1] then return 2 end
        local count = redis.call('INCR',KEYS[3])
        if count == 1 then redis.call('EXPIRE',KEYS[3],86400) end
        redis.call('SET',KEYS[2],'1','EX',60)
        redis.call('DEL',KEYS[1],KEYS[4])
        redis.call('HSET',KEYS[1],'code',ARGV[1],'userId',ARGV[2],'version',ARGV[3],'challenge',ARGV[4],'attempts','0','ready','0')
        redis.call('EXPIRE',KEYS[1],600)
        return 1
        """,
        Long.class
    );
    private static final DefaultRedisScript<Long> READY = new DefaultRedisScript<>(
        """
        if redis.call('HGET',KEYS[1],'challenge') ~= ARGV[1] then return 0 end
        redis.call('HSET',KEYS[1],'ready','1')
        redis.call('EXPIRE',KEYS[1],600)
        return 1
        """,
        Long.class
    );
    private static final DefaultRedisScript<Long> CANCEL = new DefaultRedisScript<>(
        """
        if redis.call('HGET',KEYS[1],'challenge') == ARGV[1] then return redis.call('DEL',KEYS[1]) end
        return 0
        """,
        Long.class
    );
    private static final DefaultRedisScript<Long> VERIFY = new DefaultRedisScript<>(
        """
        if redis.call('HGET',KEYS[1],'ready') ~= '1' then return 0 end
        local attempts = tonumber(redis.call('HGET',KEYS[1],'attempts') or '5')
        if attempts >= 5 then redis.call('DEL',KEYS[1]); return 0 end
        if redis.call('HGET',KEYS[1],'code') ~= ARGV[1] then
          attempts = redis.call('HINCRBY',KEYS[1],'attempts',1)
          if attempts >= 5 then redis.call('DEL',KEYS[1]) end
          return 0
        end
        local userId = redis.call('HGET',KEYS[1],'userId')
        local version = redis.call('HGET',KEYS[1],'version')
        redis.call('DEL',KEYS[1],KEYS[2])
        redis.call('HSET',KEYS[2],'token',ARGV[2],'userId',userId,'version',version)
        redis.call('EXPIRE',KEYS[2],300)
        return 1
        """,
        Long.class
    );
    private static final DefaultRedisScript<List> CONSUME = new DefaultRedisScript<>(
        """
        if redis.call('HGET',KEYS[1],'token') ~= ARGV[1] then return {} end
        local result = {redis.call('HGET',KEYS[1],'userId'),redis.call('HGET',KEYS[1],'version')}
        redis.call('DEL',KEYS[1])
        return result
        """,
        List.class
    );

    public PasswordResetOtpStore(
        StringRedisTemplate redis,
        @Value("${application.auth.otp-prefix:forme:auth:reset:}") String prefix
    ) {
        this.redis = redis;
        this.prefix = prefix;
    }

    public int issue(String email, String codeHash, long userId, long version, String challenge) {
        return execute(() ->
            redis.execute(
                ISSUE,
                List.of(key(email, "otp"), key(email, "cooldown"), key(email, "daily"), key(email, "grant")),
                codeHash,
                Long.toString(userId),
                Long.toString(version),
                challenge
            )
        ).intValue();
    }

    public boolean ready(String email, String challenge) {
        return execute(() -> redis.execute(READY, List.of(key(email, "otp")), challenge)) == 1;
    }

    public void cancel(String email, String challenge) {
        execute(() -> redis.execute(CANCEL, List.of(key(email, "otp")), challenge));
    }

    public boolean verify(String email, String codeHash, String tokenHash) {
        return (
            execute(() ->
                redis.execute(VERIFY, List.of(key(email, "otp"), key(email, "grant")), codeHash, tokenHash)
            ) == 1
        );
    }

    public Grant consume(String email, String tokenHash) {
        List<?> result = execute(() -> redis.execute(CONSUME, List.of(key(email, "grant")), tokenHash));
        if (result == null || result.size() != 2) return null;
        return new Grant(Long.parseLong(result.get(0).toString()), Long.parseLong(result.get(1).toString()));
    }

    public String key(String email, String kind) {
        return prefix + "{" + digest(email) + "}:" + kind;
    }

    public static String digest(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private <T> T execute(Supplier<T> action) {
        try {
            return action.get();
        } catch (DataAccessException ex) {
            throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Dịch vụ xác minh đang tạm gián đoạn. Vui lòng thử lại sau."
            );
        }
    }

    public record Grant(long userId, long authVersion) {}
}
