package com.learnflow.backend.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnflow.backend.ai.dto.ChatRequest;
import com.learnflow.backend.catalog.dto.ProductResponse;
import com.learnflow.backend.store.StoreInfoService;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiFashionAIClientTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private MockRestServiceServer server;
  private GeminiFashionAIClient client;
  private StoreInfoService store;

  @BeforeEach
  void setup() throws Exception {
    var builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    store = new StoreInfoService(mapper);
    client = new GeminiFashionAIClient(
      "test-key",
      "gemini-3.5-flash-lite",
      builder.build(),
      mapper,
      store
    );
  }

  static ProductResponse product(long id, int stock, String status) {
    return new ProductResponse(
      id,
      1L,
      "Áo linen công sở",
      "Linen thoáng mát",
      "FORME",
      "Linen",
      "Female",
      new BigDecimal("390000"),
      null,
      status,
      null,
      null,
      List.of(
        new ProductResponse.VariantResponse(
          id,
          "M",
          "Trắng",
          stock,
          "private-sku",
          null
        )
      ),
      List.of(),
      "SIZE_AO_NU"
    );
  }

  static ChatRequest request(String message, boolean consent) {
    return new ChatRequest(
      message,
      null,
      new BigDecimal("172"),
      new BigDecimal("62.3"),
      null,
      null,
      null,
      null,
      null,
      null,
      "Private body shape",
      consent
    );
  }

  private String response(String text) throws Exception {
    return mapper.writeValueAsString(
      Map.of(
        "candidates",
        List.of(
          Map.of(
            "finishReason",
            "STOP",
            "content",
            Map.of(
              "parts",
              List.of(
                Map.of("thought", true, "text", "hidden reasoning"),
                Map.of("text", text)
              )
            )
          )
        )
      )
    );
  }

  @Test
  void callsGoogleWithPublicContextAndReturnsItsAnswerWithoutSendingProfileFields()
    throws Exception {
    server
      .expect(
        requestTo(
          "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"
        )
      )
      .andExpect(header("x-goog-api-key", "test-key"))
      .andExpect(request -> {
        String body = ((MockClientHttpRequest) request).getBodyAsString();
        var context = mapper.readTree(
          mapper
            .readTree(body)
            .path("contents")
            .get(0)
            .path("parts")
            .get(0)
            .path("text")
            .asText()
        );
        assertEquals(1, context.path("catalog").size());
        assertEquals(
          1L,
          context.path("catalog").get(0).path("productId").asLong()
        );
        assertTrue(context.path("question").asText().contains("[email]"));
        assertTrue(context.path("question").asText().contains("[điện thoại]"));
        assertFalse(
          context.path("question").asText().contains("alice@example.com")
        );
        assertFalse(context.path("question").asText().contains("0912345678"));
        assertFalse(body.contains("heightCm"));
        assertFalse(body.contains("weightKg"));
        assertFalse(body.contains("Private body shape"));
        assertFalse(body.contains("private-sku"));
        assertTrue(context.path("storeInfo").path("policies").isArray());
        assertEquals(
          "application/json",
          mapper
            .readTree(body)
            .path("generationConfig")
            .path("responseMimeType")
            .asText()
        );
      })
      .andRespond(
        withSuccess(
          response(
            "{\"answer\":\"Bạn có thể phối áo linen với quần suông.\",\"productIds\":[1,1]}"
          ),
          MediaType.APPLICATION_JSON
        )
      );
    var result = client.advise(
      request("Tư vấn áo, alice@example.com, 0912345678", true),
      List.of(
        product(1, 2, "Active"),
        product(2, 0, "Active"),
        product(3, 2, "Inactive")
      )
    );
    assertEquals("Gemini API", result.mode());
    assertEquals("Bạn có thể phối áo linen với quần suông.", result.answer());
    assertEquals(List.of(1L), result.productIds());
    assertNull(result.suggestedSize());
    server.verify();
  }

  @ParameterizedTest
  @ValueSource(
    strings = {
      "not json",
      "{\"answer\":\"Có hàng ảo\",\"productIds\":[99999]}",
      "{\"answer\":\"Có hàng\",\"productIds\":[1.5]}",
      "{\"answer\":\"Có hàng\",\"productIds\":[\"1\"]}",
      "{\"answer\":\"\",\"productIds\":[]}",
      "{\"answer\":\"Có hàng\",\"productIds\":null}",
    }
  )
  void rejectsMalformedOrUngroundedAnswers(String text) throws Exception {
    server
      .expect(anything())
      .andRespond(withSuccess(response(text), MediaType.APPLICATION_JSON));
    assertThrows(AIProviderException.class, () ->
      client.advise(
        request("Tư vấn áo", true),
        List.of(product(1, 2, "Active"))
      )
    );
    server.verify();
  }

  @Test
  void canAnswerFashionQuestionsWithoutAttachingProducts() throws Exception {
    server
      .expect(anything())
      .andRespond(
        withSuccess(
          response(
            "{\"answer\":\"Linen thoáng mát nhưng dễ nhăn hơn cotton.\",\"productIds\":[]}"
          ),
          MediaType.APPLICATION_JSON
        )
      );
    var result = client.advise(
      request("Linen khác cotton thế nào?", true),
      List.of()
    );
    assertEquals(List.of(), result.productIds());
    assertTrue(result.answer().contains("Linen"));
    server.verify();
  }

  @Test
  void quotaFailureHasSafeUsefulMessageWithoutProviderBody() {
    server
      .expect(anything())
      .andRespond(
        withStatus(HttpStatus.TOO_MANY_REQUESTS)
          .body("secret provider body test-key")
          .contentType(MediaType.APPLICATION_JSON)
      );
    var error = assertThrows(AIProviderException.class, () ->
      client.advise(request("Tư vấn áo", true), List.of())
    );
    assertTrue(error.getMessage().contains("hạn mức"));
    assertFalse(error.getMessage().contains("test-key"));
    assertFalse(error.getMessage().contains("secret provider body"));
    server.verify();
  }

  @Test
  void timeoutDoesNotEchoTransportDetails() {
    server
      .expect(anything())
      .andRespond(
        withException(new SocketTimeoutException("private connection details"))
      );
    var error = assertThrows(AIProviderException.class, () ->
      client.advise(request("Tư vấn áo", true), List.of())
    );
    assertTrue(error.getMessage().contains("thời gian chờ"));
    assertFalse(error.getMessage().contains("private connection details"));
    server.verify();
  }

  @Test
  void absentKeyDoesNotCallGoogle() {
    var absent = new GeminiFashionAIClient(
      "",
      "gemini-3.5-flash-lite",
      RestClient.create(),
      mapper,
      store
    );
    assertFalse(absent.configured());
    assertThrows(AIProviderException.class, () ->
      absent.advise(request("Tư vấn áo", true), List.of())
    );
    server.verify();
  }
}
