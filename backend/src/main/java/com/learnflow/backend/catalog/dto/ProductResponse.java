package com.learnflow.backend.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
    Long productId,
    Long categoryId,
    String productName,
    String description,
    String brand,
    String material,
    String gender,
    BigDecimal price,
    BigDecimal salePrice,
    String status,
    Instant createdAt,
    Instant updatedAt,
    List<VariantResponse> variants,
    List<ImageResponse> images
) {
    public record VariantResponse(
        Long variantId,
        String size,
        String color,
        int stockQuantity,
        String sku,
        String imageUrl
    ) {}

    public record ImageResponse(Long imageId, String imageUrl, boolean isPrimary) {}
}
