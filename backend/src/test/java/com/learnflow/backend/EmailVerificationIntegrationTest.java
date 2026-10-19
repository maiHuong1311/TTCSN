package com.learnflow.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnflow.backend.auth.*;
import com.learnflow.backend.common.web.ApiException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

class EmailVerificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    RegistrationEmailVerificationRepository verifications;

    @Autowired
    AuthRepository users;

    @Autowired
    TransactionTemplate tx;

    @MockBean
    RegistrationMailService mail;

    private String email, username, code;

    @BeforeEach
    void setup() {
        username = "verify_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        email = username + "@example.com";
        doAnswer(i -> {
            code = i.getArgument(1);
            return null;
        })
            .when(mail)
            .sendCode(anyString(), anyString());
    }

    private void send(int statusCode) throws Exception {
        mvc.perform(
            post("/api/auth/register/email-code")
                .contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("email", email)))
        ).andExpect(status().is(statusCode));
    }

    private void register(String value, String phone, int statusCode) throws Exception {
        mvc.perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content(
                    mapper.writeValueAsString(
                        Map.of(
                            "email",
                            email,
                            "username",
                            username,
                            "password",
                            "TestPassword123",
                            "fullName",
                            "Người dùng kiểm thử",
                            "phoneNumber",
                            phone,
                            "verificationCode",
                            value
                        )
                    )
                )
        ).andExpect(status().is(statusCode));
    }

    @Test
    void cannotCreateAccountWithoutRequestingCode() throws Exception {
        register("123456", "0912345678", 400);
        assertFalse(users.existsByEmailIgnoreCase(email));
        verifyNoInteractions(mail);
    }

    @Test
    void validCodeCreatesAccountAndCannotBeReused() throws Exception {
        send(200);
        assertTrue(code.matches("[0-9]{6}"));
        assertFalse(users.existsByEmailIgnoreCase(email));
        tx.executeWithoutResult(s ->
            assertNotEquals(code, verifications.lockByEmail(email).orElseThrow().codeHash)
        );
        register(code, "+84912345678", 201);
        assertTrue(users.existsByEmailIgnoreCase(email));
        tx.executeWithoutResult(s -> assertTrue(verifications.lockByEmail(email).orElseThrow().consumed));
        register(code, "0912345678", 409);
    }

    @Test
    void fiveWrongAttemptsPersistAcrossFailedHttpRequests() throws Exception {
        send(200);
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) register(wrong, "0912345678", 400);
        register(code, "0912345678", 400);
        tx.executeWithoutResult(s ->
            assertEquals(5, verifications.lockByEmail(email).orElseThrow().attempts)
        );
        assertFalse(users.existsByEmailIgnoreCase(email));
    }

    @Test
    void expiredCodeDoesNotCreateAccount() throws Exception {
        send(200);
        tx.executeWithoutResult(
            s ->
                verifications.lockByEmail(email).orElseThrow().expiresAt = Instant.parse(
                    "2020-01-01T00:00:00Z"
                )
        );
        register(code, "0912345678", 400);
        assertFalse(users.existsByEmailIgnoreCase(email));
    }

    @Test
    void resendHasCooldownInvalidatesOldCodeAndEnforcesDailyLimit() throws Exception {
        send(200);
        String oldCode = code;
        send(429);
        tx.executeWithoutResult(
            s -> verifications.lockByEmail(email).orElseThrow().sentAt = Instant.parse("2020-01-01T00:00:00Z")
        );
        send(200);
        if (!code.equals(oldCode)) register(oldCode, "0912345678", 400);
        tx.executeWithoutResult(s -> {
            var v = verifications.lockByEmail(email).orElseThrow();
            v.sentAt = Instant.parse("2020-01-01T00:00:00Z");
            v.sendCount = 10;
        });
        send(429);
        register(code, "0912345678", 201);
    }

    @Test
    void invalidVietnamesePhoneAndMissingCodeReturnVietnameseErrors() throws Exception {
        mvc.perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content(
                    mapper.writeValueAsString(
                        Map.of(
                            "email",
                            email,
                            "username",
                            username,
                            "password",
                            "TestPassword123",
                            "fullName",
                            "Khách hàng",
                            "phoneNumber",
                            "+12025550123"
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.errors.phoneNumber").value(org.hamcrest.Matchers.containsString("Việt Nam"))
            )
            .andExpect(jsonPath("$.errors.verificationCode").value("Không được để trống."));
        verifyNoInteractions(mail);
    }

    @Test
    void blocksKnownDisposableDomains() throws Exception {
        email = username + "@mailinator.com";
        send(400);
        verifyNoInteractions(mail);
    }

    @Test
    void sendingFailureRollsBackChallengeAndDoesNotCreateUser() throws Exception {
        doThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể gửi email xác minh."))
            .when(mail)
            .sendCode(anyString(), anyString());
        send(503);
        tx.executeWithoutResult(s -> assertTrue(verifications.lockByEmail(email).isEmpty()));
        assertFalse(users.existsByEmailIgnoreCase(email));
    }
}
