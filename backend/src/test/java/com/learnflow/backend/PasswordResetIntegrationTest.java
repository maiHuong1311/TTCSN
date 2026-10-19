package com.learnflow.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.learnflow.backend.auth.*;
import com.learnflow.backend.common.web.ApiException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

class PasswordResetIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    AuthRepository users;

    @Autowired
    TransactionTemplate tx;

    @MockBean
    RegistrationMailService mail;

    @MockBean
    PasswordResetOtpStore store;

    private String email, registrationCode, otp, jwt, codeHash;
    private long userId, version;
    private final AtomicBoolean available = new AtomicBoolean();
    private final AtomicReference<String> grantHash = new AtomicReference<>();

    @BeforeEach
    void account() throws Exception {
        String username = "reset_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        email = username + "@example.com";
        doAnswer(i -> {
            registrationCode = i.getArgument(1);
            return null;
        })
            .when(mail)
            .sendCode(anyString(), anyString());
        doAnswer(i -> {
            otp = i.getArgument(1);
            return null;
        })
            .when(mail)
            .sendResetCode(anyString(), anyString());
        when(store.issue(anyString(), anyString(), anyLong(), anyLong(), anyString())).thenAnswer(i -> {
            codeHash = i.getArgument(1);
            version = i.getArgument(3);
            available.set(false);
            grantHash.set(null);
            return 1;
        });
        when(store.ready(anyString(), anyString())).thenAnswer(i -> {
            available.set(true);
            return true;
        });
        when(store.verify(anyString(), anyString(), anyString())).thenAnswer(i -> {
            if (
                !email.equals(i.getArgument(0)) ||
                !Objects.equals(codeHash, i.getArgument(1)) ||
                !available.compareAndSet(true, false)
            ) return false;
            grantHash.set(i.getArgument(2));
            return true;
        });
        when(store.consume(anyString(), anyString())).thenAnswer(i -> {
            String hash = i.getArgument(1);
            synchronized (grantHash) {
                if (email.equals(i.getArgument(0)) && Objects.equals(grantHash.get(), hash) && hash != null) {
                    grantHash.set(null);
                    return new PasswordResetOtpStore.Grant(userId, version);
                }
                return null;
            }
        });
        postJson("/auth/register/email-code", Map.of("email", email), 200);
        var registered = postJson(
            "/auth/register",
            Map.of(
                "email",
                email,
                "username",
                username,
                "password",
                "OldPassword123",
                "fullName",
                "Khách kiểm thử",
                "phoneNumber",
                "0912345678",
                "verificationCode",
                registrationCode
            ),
            201
        );
        jwt = registered.get("accessToken").asText();
        userId = registered.get("user").get("userId").asLong();
    }

    private JsonNode postJson(String path, Object payload, int expected) throws Exception {
        return mapper.readTree(
            mvc
                .perform(
                    post("/api" + path)
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(payload))
                )
                .andExpect(status().is(expected))
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    private void request() throws Exception {
        postJson("/auth/forgot-password", Map.of("email", email), 200);
    }

    private String verifyCode() throws Exception {
        return postJson("/auth/reset-password/verify", Map.of("email", email, "otp", otp), 200)
            .get("token")
            .asText();
    }

    private void reset(String token, int status) throws Exception {
        postJson(
            "/auth/reset-password",
            Map.of("email", email, "token", token, "password", "NewPassword123"),
            status
        );
    }

    @Test
    void otpThenPasswordChangeInvalidatesOldSessions() throws Exception {
        request();
        assertTrue(otp.matches("[0-9]{6}"));
        assertNotEquals(otp, codeHash);
        verify(mail).sendResetCode(eq(email), eq(otp));
        postJson("/auth/login", Map.of("identifier", email, "password", "OldPassword123"), 200);
        String token = verifyCode();
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        assertNotEquals(token, grantHash.get());
        reset(token, 200);
        reset(token, 400);
        postJson("/auth/login", Map.of("identifier", email, "password", "OldPassword123"), 401);
        var login = postJson("/auth/login", Map.of("identifier", email, "password", "NewPassword123"), 200);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt)).andExpect(
            status().isUnauthorized()
        );
        mvc.perform(
            get("/api/auth/me").header("Authorization", "Bearer " + login.get("accessToken").asText())
        ).andExpect(status().isOk());
    }

    @Test
    void wrongOtpDoesNotChangePasswordAndSuccessfulOtpCannotBeReused() throws Exception {
        request();
        String wrong = otp.equals("000000") ? "111111" : "000000";
        postJson("/auth/reset-password/verify", Map.of("email", email, "otp", wrong), 400);
        verifyCode();
        postJson("/auth/reset-password/verify", Map.of("email", email, "otp", otp), 400);
        postJson("/auth/login", Map.of("identifier", email, "password", "OldPassword123"), 200);
    }

    @Test
    void unknownBlockedAndRateLimitedEmailsUseSameResponse() throws Exception {
        var unknown = postJson("/auth/forgot-password", Map.of("email", "unknown_" + email), 200);
        verify(mail, never()).sendResetCode(anyString(), anyString());
        var known = postJson("/auth/forgot-password", Map.of("email", email), 200);
        assertEquals(unknown, known);
        when(store.issue(anyString(), anyString(), anyLong(), anyLong(), anyString())).thenReturn(0);
        assertEquals(known, postJson("/auth/forgot-password", Map.of("email", email), 200));
        tx.executeWithoutResult(s -> users.lockById(userId).orElseThrow().setStatus("Blocked"));
        assertEquals(known, postJson("/auth/forgot-password", Map.of("email", email), 200));
        verify(mail, times(1)).sendResetCode(anyString(), anyString());
    }

    @Test
    void missingOrExpiredVerificationCannotChangePassword() throws Exception {
        reset("A".repeat(43), 400);
        request();
        String token = verifyCode();
        grantHash.set(null);
        reset(token, 400);
        postJson("/auth/login", Map.of("identifier", email, "password", "OldPassword123"), 200);
    }

    @Test
    void blockedAccountCannotUsePreviouslyVerifiedOtp() throws Exception {
        request();
        String token = verifyCode();
        tx.executeWithoutResult(s -> users.lockById(userId).orElseThrow().setStatus("Blocked"));
        reset(token, 400);
    }

    @Test
    void sendingFailureDoesNotActivateOtp() throws Exception {
        doThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể gửi email."))
            .when(mail)
            .sendResetCode(anyString(), anyString());
        request();
        verify(store).cancel(eq(email), anyString());
        verify(store, never()).ready(anyString(), anyString());
        postJson("/auth/reset-password/verify", Map.of("email", email, "otp", "123456"), 400);
    }

    @Test
    void redisFailureReturns503WithoutSendingMail() throws Exception {
        when(store.issue(anyString(), anyString(), anyLong(), anyLong(), anyString())).thenThrow(
            new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ xác minh đang tạm gián đoạn.")
        );
        postJson("/auth/forgot-password", Map.of("email", email), 503);
        verify(mail, never()).sendResetCode(anyString(), anyString());
    }

    @Test
    void concurrentPasswordChangesConsumeVerificationOnlyOnce() throws Exception {
        request();
        String token = verifyCode();
        String payload = mapper.writeValueAsString(
            Map.of("email", email, "token", token, "password", "ConcurrentPassword123")
        );
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> change = () ->
                mvc
                    .perform(
                        post("/api/auth/reset-password").contentType("application/json").content(payload)
                    )
                    .andReturn()
                    .getResponse()
                    .getStatus();
            var results = executor.invokeAll(List.of(change, change));
            var statuses = new ArrayList<Integer>();
            for (var r : results) statuses.add(r.get(15, TimeUnit.SECONDS));
            Collections.sort(statuses);
            assertEquals(List.of(200, 400), statuses);
        } finally {
            executor.shutdownNow();
        }
    }
}
