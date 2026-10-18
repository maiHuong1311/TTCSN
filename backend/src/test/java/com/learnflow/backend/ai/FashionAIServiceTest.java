package com.learnflow.backend.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learnflow.backend.ai.dto.ChatRequest;
import com.learnflow.backend.catalog.CatalogService;
import com.learnflow.backend.catalog.dto.ProductResponse;
import com.learnflow.backend.common.web.PageResponse;
import com.learnflow.backend.order.OrderService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class FashionAIServiceTest {

    @Test
    void fabricatedProductIdsAndSizesAreRejected() {
        var provider = mock(FashionAIProvider.class);
        var catalog = mock(CatalogService.class);
        var orders = mock(OrderService.class);
        var request = new ChatRequest("A summer outfit", null, null, null);
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
            List.of(),
            List.of()
        );
        when(catalog.list("", null, null, 0, 100, false)).thenReturn(
            new PageResponse<>(List.of(p), 0, 100, 1, 1)
        );
        when(provider.configured()).thenReturn(true);
        when(provider.advise(request, List.of(p))).thenReturn(
            new FashionAIProvider.Selection(List.of(1L, 99999L), "invented")
        );
        var result = new FashionAIService(provider, catalog, orders).chat(1L, request);
        assertEquals(1, result.products().size());
        assertEquals(1L, result.products().get(0).productId());
        assertNull(result.suggestedSize());
    }
}
