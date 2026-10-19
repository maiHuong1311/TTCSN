package com.learnflow.backend.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long orderId,
    Long userId,
    String receiverName,
    String receiverPhone,
    String shippingAddress,
    BigDecimal totalAmount,
    BigDecimal shippingFee,
    BigDecimal discountAmount,
    String orderStatus,
    String note,
    Instant createdAt,
    List<ItemResponse> items,
    PaymentResponse payment
) {
    public record ItemResponse(
        Long orderItemId,
        Long variantId,
        Long productId,
        String productName,
        String size,
        String color,
        int quantity,
        BigDecimal unitPrice
    ) {}

    public record PaymentResponse(
        Long paymentId,
        String paymentMethod,
        String paymentStatus,
        BigDecimal amount,
        String transactionId,
        Instant paidAt
    ) {}
}
