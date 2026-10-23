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
  private final GeminiFashionAIClient gemini;

  public HttpFashionAIProvider(
    @Value("${application.ai.base-url:}") String url,
    @Value("${application.ai.api-key:}") String key,
    @Value("${application.ai.connect-timeout-seconds:3}") int connect,
    @Value("${application.ai.read-timeout-seconds:30}") int read,
    GeminiFashionAIClient gemini
  ) {
    this.url = url;
    this.key = key;
    this.gemini = gemini;
    var f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(connect * 1000);
    f.setReadTimeout(read * 1000);
    client = RestClient.builder().requestFactory(f).build();
  }

  public boolean configured() {
    return gemini.configured() || !url.isBlank();
  }

  @Override
  public com.learnflow.backend.ai.dto.AiStatusResponse status() {
    // Configuration is not a billable model request; the chat result confirms successful use.
    if (
      gemini.configured()
    ) return new com.learnflow.backend.ai.dto.AiStatusResponse(
      true,
      "Gemini API (đã cấu hình)"
    );
    if (!configured()) return new com.learnflow.backend.ai.dto.AiStatusResponse(
      false,
      "Chưa cấu hình dịch vụ AI"
    );
    try {
      var health = client
        .get()
        .uri(url + "/health")
        .header("Authorization", "Bearer " + key)
        .retrieve()
        .body(Health.class);
      return new com.learnflow.backend.ai.dto.AiStatusResponse(
        health != null && health.geminiConfigured(),
        health != null && health.geminiConfigured()
          ? "Gemini + RAG"
          : "Gợi ý local; Gemini chưa có key"
      );
    } catch (Exception ex) {
      return new com.learnflow.backend.ai.dto.AiStatusResponse(
        false,
        "Dịch vụ AI local chưa chạy"
      );
    }
  }

  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
  public record Health(boolean geminiConfigured) {}

  public Selection advise(ChatRequest r, List<ProductResponse> catalog) {
    if (gemini.configured()) return gemini.advise(r, catalog);
    return client
      .post()
      .uri(url + "/fashion/advise")
      .header("Authorization", "Bearer " + key)
      .body(new ProviderRequest(r.message(), catalog))
      .retrieve()
      .body(Selection.class);
  }

  public record ProviderRequest(
    String message,
    List<ProductResponse> catalog
  ) {}
}
