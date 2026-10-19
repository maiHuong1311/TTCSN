package com.learnflow.backend.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(Long cartId, List<ItemResponse> items, BigDecimal subtotal) {
    public record ItemResponse(
        Long cartItemId,
        Long variantId,
        Long productId,
        String productName,
        String size,
        String color,
        int quantity,
        int stockQuantity,
        BigDecimal unitPrice,
        String imageUrl
    ) {}
}
