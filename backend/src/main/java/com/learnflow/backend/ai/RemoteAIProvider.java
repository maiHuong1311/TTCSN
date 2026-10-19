package com.learnflow.backend.ai;

import com.learnflow.backend.prediction.dto.PredictionRequest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class RemoteAIProvider implements AIProvider {
    private final String baseUrl; private final String apiKey; private final RestTemplate client;
    public RemoteAIProvider(RestTemplateBuilder builder,
        @Value("${application.ai.base-url:}") String baseUrl,
        @Value("${application.ai.api-key:}") String apiKey,
        @Value("${application.ai.connect-timeout-seconds:3}") long connectTimeout,
        @Value("${application.ai.read-timeout-seconds:10}") long readTimeout) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", ""); this.apiKey = apiKey;
        this.client = builder.setConnectTimeout(Duration.ofSeconds(connectTimeout)).setReadTimeout(Duration.ofSeconds(readTimeout)).build();
    }

    @Override public AiPrediction predict(PredictionRequest request) {
        if (baseUrl.isBlank()) throw new AIProviderException("AI_API_URL is not configured");
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) headers.setBearerAuth(apiKey);
        try {
            ResponseEntity<AiPrediction> response = client.exchange(baseUrl + "/predict", HttpMethod.POST, new HttpEntity<>(request, headers), AiPrediction.class);
            if (response.getBody() == null) throw new AIProviderException("AI API returned an empty response");
            return response.getBody();
        } catch (RestClientException exception) { throw new AIProviderException("AI API is unavailable", exception); }
    }
}
