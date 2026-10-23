package com.learnflow.backend.catalog;

import static org.junit.jupiter.api.Assertions.*;

import com.learnflow.backend.catalog.dto.ProductResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class RecommendationEngineTest {

    private final RecommendationEngine engine = new RecommendationEngine();

    private ProductResponse p(
        long id,
        long category,
        String name,
        String material,
        int stock
    ) {
        return new ProductResponse(
            id,
            category,
            name,
            name,
            "FORME",
            material,
            "Female",
            new BigDecimal("300000"),
            null,
            "Active",
            Instant.parse("2026-10-01T00:00:00Z"),
            null,
            List.of(
                new ProductResponse.VariantResponse(
                    id,
                    "M",
                    "Trắng",
                    stock,
                    "SKU" + id,
                    null
                )
            ),
            List.of(),
            null
        );
    }

    @Test
    void coldStartUsesAnonymousDeliveredPurchasePopularityAndExcludesOutOfStock() {
        var ranked = engine.rank(
            List.of(
                p(1, 1, "Áo linen", "Linen", 2),
                p(2, 2, "Quần tây", "Cotton", 3),
                p(3, 3, "Đầm", "Satin", 0)
            ),
            Map.of(),
            "",
            Map.of(2L, 20L, 3L, 100L),
            Map.of(),
            null,
            false
        );
        assertEquals(
            List.of(2L, 1L),
            ranked.stream().map(ProductResponse::productId).toList()
        );
    }

    @Test
    void historyAndSearchChangeContentBasedRanking() {
        var all = List.of(
            p(1, 1, "Áo linen đi làm", "Linen", 2),
            p(2, 1, "Áo linen công sở", "Linen", 3),
            p(3, 3, "Đầm satin dự tiệc", "Satin", 3)
        );
        var ranked = engine.rank(
            all,
            Map.of(1L, 5.0),
            "",
            Map.of(),
            Map.of(),
            null,
            false
        );
        assertTrue(ranked.indexOf(all.get(1)) < ranked.indexOf(all.get(2)));
        ranked = engine.rank(
            all,
            Map.of(),
            "đầm satin",
            Map.of(),
            Map.of(),
            null,
            false
        );
        assertEquals(3L, ranked.get(0).productId());
    }

    @Test
    void mixAndMatchChoosesComplementaryGroupsAndUsesActualCoPurchaseSignal() {
        var all = List.of(
            p(1, 1, "Áo sơ mi", "Cotton", 2),
            p(2, 1, "Áo kiểu", "Cotton", 3),
            p(3, 2, "Quần tây", "Cotton", 3),
            p(4, 3, "Đầm satin", "Satin", 3),
            p(5, 4, "Túi xách", "Da", 3)
        );
        var ranked = engine.rank(
            all,
            Map.of(),
            "",
            Map.of(),
            Map.of(3L, 10L),
            1L,
            true
        );
        assertEquals(3L, ranked.get(0).productId());
        assertTrue(
            ranked
                .stream()
                .noneMatch(p -> Set.of(1L, 2L, 4L).contains(p.productId()))
        );
    }
}
