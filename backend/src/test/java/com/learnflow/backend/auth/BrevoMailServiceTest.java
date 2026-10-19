package com.learnflow.backend.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.learnflow.backend.common.web.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BrevoMailServiceTest {

    @Test
    void sendsTransactionalEmailUsingBackendApiKey() {
        var builder = RestClient.builder().baseUrl("https://api.brevo.com/v3");
        var server = MockRestServiceServer.bindTo(builder).build();
        server
            .expect(requestTo("https://api.brevo.com/v3/smtp/email"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("api-key", "test-only-key"))
            .andExpect(jsonPath("$.sender.email").value("shop@example.com"))
            .andExpect(jsonPath("$.to[0].email").value("customer@example.com"))
            .andExpect(jsonPath("$.textContent").value("Mã xác minh 012345"))
            .andRespond(
                withStatus(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"messageId\":\"test-id\"}")
            );
        var mail = new BrevoMailService(builder.build(), "test-only-key", "shop@example.com");
        assertTrue(mail.configured());
        mail.send("customer@example.com", "Xác minh FORME", "Mã xác minh 012345");
        server.verify();
    }

    @Test
    void providerErrorsAreVietnameseAndDoNotExposeKeyOrResponse() {
        var builder = RestClient.builder().baseUrl("https://api.brevo.com/v3");
        var server = MockRestServiceServer.bindTo(builder).build();
        server
            .expect(requestTo("https://api.brevo.com/v3/smtp/email"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("provider-private-detail"));
        var mail = new BrevoMailService(builder.build(), "test-only-key", "shop@example.com");
        var error = assertThrows(ApiException.class, () ->
            mail.send("customer@example.com", "Subject", "Text")
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.status());
        assertTrue(error.getMessage().contains("Không thể gửi email qua Brevo"));
        assertFalse(error.getMessage().contains("test-only-key"));
        assertFalse(error.getMessage().contains("provider-private-detail"));
        server.verify();
    }

    @SuppressWarnings("unchecked")
    @Test
    void registrationAndResetUseBrevoWithoutSmtpWhenApiIsConfigured() {
        var provider = (ObjectProvider<JavaMailSender>) mock(ObjectProvider.class);
        var brevo = mock(BrevoMailService.class);
        when(brevo.configured()).thenReturn(true);
        var mail = new RegistrationMailService(provider, "", brevo);
        mail.sendCode("customer@example.com", "123456");
        mail.sendResetCode("customer@example.com", "012345");
        verify(brevo).send(eq("customer@example.com"), contains("xác minh"), contains("123456"));
        verify(brevo).send(eq("customer@example.com"), contains("Đặt lại mật khẩu"), contains("012345"));
    }
}
