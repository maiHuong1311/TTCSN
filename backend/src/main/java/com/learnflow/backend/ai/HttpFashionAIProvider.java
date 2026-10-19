package com.learnflow.backend.ai;

import com.learnflow.backend.ai.dto.ChatRequest;
import com.learnflow.backend.catalog.dto.ProductResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpFashionAIProvider implements FashionAIProvider {

    private final String url, key;
    private final RestClient client;

    public HttpFashionAIProvider(
        @Value("${application.ai.base-url:}") String url,
        @Value("${application.ai.api-key:}") String key,
        @Value("${application.ai.connect-timeout-seconds:3}") int connect,
        @Value("${application.ai.read-timeout-seconds:10}") int read
    ) {
        this.url = url;
        this.key = key;
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(connect * 1000);
        f.setReadTimeout(read * 1000);
        client = RestClient.builder().requestFactory(f).build();
    }

    public boolean configured() {
        return !url.isBlank();
    }

    public Selection advise(ChatRequest r, List<ProductResponse> catalog) {
        return client
            .post()
            .uri(url + "/fashion/advise")
            .header("Authorization", "Bearer " + key)
            .body(new ProviderRequest(r.message(), r.heightCm(), r.weightKg(), catalog))
            .retrieve()
            .body(Selection.class);
    }

    public record ProviderRequest(
        String message,
        java.math.BigDecimal heightCm,
        java.math.BigDecimal weightKg,
        List<ProductResponse> catalog
    ) {}
}
