package com.learnflow.backend.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnflow.backend.auth.domain.AppUser;
import com.learnflow.backend.common.web.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final ObjectMapper mapper;
    private final Clock clock;
    private final byte[] secret;
    private final long minutes;

    public JwtService(
        ObjectMapper mapper,
        Clock clock,
        @Value("${application.jwt.secret}") String secret,
        @Value("${application.jwt.access-token-minutes}") long minutes
    ) {
        this.mapper = mapper;
        this.clock = clock;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.minutes = minutes;
    }

    public Token issue(AppUser user) {
        Instant expiresAt = clock.instant().plus(Duration.ofMinutes(minutes));
        try {
            String header = encode(mapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = encode(
                mapper.writeValueAsBytes(
                    Map.of(
                        "sub",
                        user.getId(),
                        "email",
                        user.getEmail(),
                        "role",
                        user.getRole().name(),
                        "ver",
                        user.getAuthVersion(),
                        "exp",
                        expiresAt.getEpochSecond()
                    )
                )
            );
            String content = header + "." + payload;
            return new Token(content + "." + encode(sign(content)), expiresAt);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create access token", exception);
        }
    }

    public Claims verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException();
            byte[] expected = sign(parts[0] + "." + parts[1]);
            if (
                !MessageDigest.isEqual(expected, DECODER.decode(parts[2]))
            ) throw new IllegalArgumentException();
            Map<String, Object> values = mapper.readValue(DECODER.decode(parts[1]), new TypeReference<>() {});
            long expires = ((Number) values.get("exp")).longValue();
            if (
                !Instant.ofEpochSecond(expires).isAfter(clock.instant())
            ) throw new IllegalArgumentException();
            return new Claims(
                ((Number) values.get("sub")).longValue(),
                values.get("email").toString(),
                values.get("role").toString(),
                ((Number) values.getOrDefault("ver", 0)).longValue()
            );
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        }
    }

    private byte[] sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) {
        return ENCODER.encodeToString(value);
    }

    public record Token(String value, Instant expiresAt) {}

    public record Claims(Long userId, String email, String role, long authVersion) {}
}
