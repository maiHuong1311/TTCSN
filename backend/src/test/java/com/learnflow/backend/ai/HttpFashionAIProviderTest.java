package com.learnflow.backend.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class HttpFashionAIProviderTest {

  @Test
  void configuredGeminiTakesPriorityOverLegacyPythonWithoutCallingItsHealthEndpoint() {
    var gemini = mock(GeminiFashionAIClient.class);
    when(gemini.configured()).thenReturn(true);
    var provider = new HttpFashionAIProvider(
      "http://127.0.0.1:1",
      "legacy-token",
      1,
      1,
      gemini
    );
    assertTrue(provider.configured());
    assertTrue(provider.status().configured());
    assertEquals("Gemini API (đã cấu hình)", provider.status().mode());
    var request = GeminiFashionAIClientTest.request("Tư vấn phối đồ", true);
    var answer = new FashionAIProvider.Selection(
      List.of(),
      null,
      null,
      "Gemini API",
      null,
      "Tư vấn thời trang."
    );
    when(gemini.advise(request, List.of())).thenReturn(answer);
    assertEquals(answer, provider.advise(request, List.of()));
    verify(gemini).advise(request, List.of());
  }

  @Test
  void absentConfigurationDoesNotConnectToAnyService() {
    var provider = new HttpFashionAIProvider(
      "",
      "",
      1,
      1,
      mock(GeminiFashionAIClient.class)
    );
    assertFalse(provider.configured());
    assertFalse(provider.status().configured());
  }

  @Test
  void legacyServiceRemainsAvailableWhenNoGeminiKeyIsSet() {
    var provider = new HttpFashionAIProvider(
      "http://127.0.0.1:8001",
      "legacy-token",
      1,
      1,
      mock(GeminiFashionAIClient.class)
    );
    assertTrue(provider.configured());
  }
}
