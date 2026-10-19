package com.learnflow.backend.auth;

import com.learnflow.backend.common.web.ApiException;
import java.util.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

@Service
public class BrevoMailService {

    private final RestClient client;
    private final String apiKey;
    private final String sender;

    public BrevoMailService(
        @Qualifier("brevoRestClient") RestClient client,
        @Value("${application.mail.brevo-api-key:}") String apiKey,
        @Value("${application.mail.brevo-sender-email:}") String sender
    ) {
        this.client = client;
        this.apiKey = apiKey;
        this.sender = sender;
    }

    public boolean configured() {
        return !apiKey.isBlank() && !sender.isBlank();
    }

    public void send(String to, String subject, String text) {
        if (!configured()) throw new ApiException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Chưa cấu hình dịch vụ gửi email. Vui lòng liên hệ quản trị viên."
        );
        try {
            client
                .post()
                .uri("/smtp/email")
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    Map.of(
                        "sender",
                        Map.of("email", sender, "name", "FORME"),
                        "to",
                        List.of(Map.of("email", to)),
                        "subject",
                        subject,
                        "textContent",
                        text
                    )
                )
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Không thể gửi email qua Brevo. Vui lòng thử lại sau."
            );
        }
    }
}
