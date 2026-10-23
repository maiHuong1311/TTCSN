package com.learnflow.backend.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learnflow.backend.ai.dto.ChatRequest;
import com.learnflow.backend.catalog.CatalogService;
import com.learnflow.backend.catalog.dto.ProductResponse;
import com.learnflow.backend.order.OrderService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class FashionAIServiceTest {

  private final FashionAIProvider provider = mock(FashionAIProvider.class);
  private final CatalogService catalog = mock(CatalogService.class);
  private final OrderService orders = mock(OrderService.class);
  private final com.learnflow.backend.store.StoreInfoService store = mock(
    com.learnflow.backend.store.StoreInfoService.class
  );
  private final com.learnflow.backend.store.SizeGuideService sizes = mock(
    com.learnflow.backend.store.SizeGuideService.class
  );
  private final FashionAIService service = new FashionAIService(
    provider,
    catalog,
    orders,
    store,
    sizes
  );

  @Test
  void displaysDirectGeminiAdviceAndKeepsOversizeAsStylingIntent() {
    var p = GeminiFashionAIClientTest.product(1, 2, "Active");
    var request = GeminiFashionAIClientTest.request(
      "Áo oversize phối thế nào?",
      true
    );
    when(provider.configured()).thenReturn(true);
    when(catalog.availableCatalog()).thenReturn(List.of(p));
    when(catalog.get(1L)).thenReturn(p);
    when(provider.advise(request, List.of(p))).thenReturn(
      new FashionAIProvider.Selection(
        List.of(1L),
        null,
        null,
        "Gemini API",
        null,
        "Phối áo rộng với quần gọn để cân bằng tỷ lệ."
      )
    );
    var result = service.chat(1L, request);
    assertEquals("Gemini API", result.mode());
    assertEquals(
      "Phối áo rộng với quần gọn để cân bằng tỷ lệ.",
      result.answer()
    );
    verifyNoInteractions(sizes);
  }

  @Test
  void generalFashionAdviceCanHaveNoProductCards() {
    var request = GeminiFashionAIClientTest.request(
      "Linen khác cotton thế nào?",
      true
    );
    when(provider.configured()).thenReturn(true);
    when(catalog.availableCatalog()).thenReturn(List.of());
    when(provider.advise(request, List.of())).thenReturn(
      new FashionAIProvider.Selection(
        List.of(),
        null,
        null,
        "Gemini API",
        null,
        "Linen thoáng mát và dễ nhăn."
      )
    );
    var result = service.chat(1L, request);
    assertEquals("Linen thoáng mát và dễ nhăn.", result.answer());
    assertTrue(result.products().isEmpty());
  }

  @Test
  void explicitOptInIsRequiredBeforeCallingTheModel() {
    var request = GeminiFashionAIClientTest.request("Tư vấn phối đồ", false);
    when(catalog.contextSuggestions(request.message())).thenReturn(List.of());
    assertEquals(
      "Gợi ý theo dữ liệu cửa hàng",
      service.chat(1L, request).mode()
    );
    verifyNoInteractions(provider);
  }

  @Test
  void questionBudgetFiltersProductsBeforeCallingGemini() {
    var expensive = GeminiFashionAIClientTest.product(1, 2, "Active");
    var request = GeminiFashionAIClientTest.request("Tư vấn áo dưới 1đ", true);
    when(provider.configured()).thenReturn(true);
    when(catalog.maximumPrice(request.message())).thenReturn(BigDecimal.ONE);
    when(catalog.availableCatalog()).thenReturn(List.of(expensive));
    when(provider.advise(request, List.of())).thenReturn(
      new FashionAIProvider.Selection(
        List.of(),
        null,
        null,
        "Gemini API",
        null,
        "Chưa có sản phẩm trong ngân sách."
      )
    );
    assertTrue(service.chat(1L, request).products().isEmpty());
    verify(provider).advise(request, List.of());
  }

  @Test
  void removesAdviceWhenStockChangesAfterTheModelRequest() {
    var p = GeminiFashionAIClientTest.product(1, 2, "Active");
    var request = GeminiFashionAIClientTest.request("Tư vấn áo", true);
    when(provider.configured()).thenReturn(true);
    when(catalog.availableCatalog()).thenReturn(List.of(p));
    when(catalog.get(1L)).thenReturn(
      GeminiFashionAIClientTest.product(1, 0, "Active")
    );
    when(provider.advise(request, List.of(p))).thenReturn(
      new FashionAIProvider.Selection(
        List.of(1L),
        null,
        null,
        "Gemini API",
        null,
        "Nội dung đã cũ, vẫn giới thiệu áo."
      )
    );
    var result = service.chat(1L, request);
    assertTrue(result.products().isEmpty());
    assertFalse(result.answer().contains("Nội dung đã cũ"));
  }

  @Test
  void providerFailureReturnsClearlyLabelledLocalSuggestions() {
    var request = GeminiFashionAIClientTest.request("Tư vấn áo", true);
    when(provider.configured()).thenReturn(true);
    when(catalog.availableCatalog()).thenReturn(List.of());
    when(provider.advise(request, List.of())).thenThrow(
      new AIProviderException("Gemini đang hết hạn mức.")
    );
    when(catalog.contextSuggestions(request.message())).thenReturn(List.of());
    var result = service.chat(1L, request);
    assertEquals("Gợi ý local", result.mode());
    assertTrue(result.answer().contains("hạn mức"));
  }

  @Test
  void rechecksQuestionBudgetIfTheProductPriceChanges() {
    var p = GeminiFashionAIClientTest.product(1, 2, "Active");
    var request = GeminiFashionAIClientTest.request(
      "Tư vấn áo dưới 500 nghìn",
      true
    );
    when(provider.configured()).thenReturn(true);
    when(catalog.maximumPrice(request.message())).thenReturn(
      new BigDecimal("500000")
    );
    when(catalog.availableCatalog()).thenReturn(List.of(p));
    var changed = new ProductResponse(
      p.productId(),
      p.categoryId(),
      p.productName(),
      p.description(),
      p.brand(),
      p.material(),
      p.gender(),
      new BigDecimal("600000"),
      null,
      p.status(),
      p.createdAt(),
      p.updatedAt(),
      p.variants(),
      p.images(),
      p.sizeChartCode()
    );
    when(catalog.get(1L)).thenReturn(changed);
    when(provider.advise(request, List.of(p))).thenReturn(
      new FashionAIProvider.Selection(
        List.of(1L),
        null,
        null,
        "Gemini API",
        null,
        "Nội dung với giá đã cũ."
      )
    );
    var result = service.chat(1L, request);
    assertTrue(result.products().isEmpty());
    assertFalse(result.answer().contains("Nội dung với giá đã cũ"));
  }

  @Test
  void fabricatedProductIdsAndSizesAreRejected() {
    var provider = mock(FashionAIProvider.class);
    var catalog = mock(CatalogService.class);
    var orders = mock(OrderService.class);
    var request = new ChatRequest(
      "A summer outfit",
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      true
    );
    ProductResponse p = new ProductResponse(
      1L,
      1L,
      "Real shirt",
      "",
      "FORME",
      "Linen",
      "Unisex",
      BigDecimal.TEN,
      null,
      "Active",
      null,
      null,
      List.of(
        new ProductResponse.VariantResponse(1L, "M", "White", 1, "sku", null)
      ),
      List.of(),
      "SIZE_AO_NU"
    );
    when(catalog.availableCatalog()).thenReturn(List.of(p));
    when(catalog.get(1L)).thenReturn(p);
    when(provider.configured()).thenReturn(true);
    when(provider.advise(request, List.of(p))).thenReturn(
      new FashionAIProvider.Selection(List.of(1L, 99999L), "invented")
    );
    var result = new FashionAIService(
      provider,
      catalog,
      orders,
      mock(com.learnflow.backend.store.StoreInfoService.class),
      mock(com.learnflow.backend.store.SizeGuideService.class)
    ).chat(1L, request);
    assertEquals(1, result.products().size());
    assertEquals(1L, result.products().get(0).productId());
    assertNull(result.suggestedSize());
  }
}
