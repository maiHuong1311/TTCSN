package com.learnflow.backend.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learnflow.backend.common.web.ApiException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;

class RegistrationMailServiceTest {

    @SuppressWarnings("unchecked")
    @Test
    void sendsCodeOnlyToRequestedRecipient() {
        var provider = (ObjectProvider<JavaMailSender>) mock(ObjectProvider.class);
        var sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        new RegistrationMailService(provider, "shop@example.com", mock(BrevoMailService.class)).sendCode(
            "customer@example.com",
            "012345"
        );
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertArrayEquals(new String[] { "customer@example.com" }, message.getValue().getTo());
        assertEquals("shop@example.com", message.getValue().getFrom());
        assertTrue(message.getValue().getText().contains("012345"));
        assertTrue(message.getValue().getSubject().contains("xác minh"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void missingConfigurationAndDeliveryFailureReturnVietnamese503() {
        var provider = (ObjectProvider<JavaMailSender>) mock(ObjectProvider.class);
        var service = new RegistrationMailService(provider, "", mock(BrevoMailService.class));
        var missing = assertThrows(ApiException.class, () ->
            service.sendCode("customer@example.com", "012345")
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, missing.status());
        assertTrue(missing.getMessage().contains("Chưa cấu hình"));
        var sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        doThrow(new MailSendException("SMTP failed")).when(sender).send(any(SimpleMailMessage.class));
        var configured = new RegistrationMailService(
            provider,
            "shop@example.com",
            mock(BrevoMailService.class)
        );
        var failed = assertThrows(ApiException.class, () ->
            configured.sendCode("customer@example.com", "012345")
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, failed.status());
        assertTrue(failed.getMessage().contains("Không thể gửi email"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void sendsPasswordResetOtpToAccountEmail() {
        var provider = (ObjectProvider<JavaMailSender>) mock(ObjectProvider.class);
        var sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        new RegistrationMailService(provider, "shop@example.com", mock(BrevoMailService.class)).sendResetCode(
            "customer@example.com",
            "012345"
        );
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertArrayEquals(new String[] { "customer@example.com" }, message.getValue().getTo());
        assertTrue(message.getValue().getText().contains("012345"));
        assertTrue(message.getValue().getSubject().contains("Đặt lại mật khẩu"));
    }
}
