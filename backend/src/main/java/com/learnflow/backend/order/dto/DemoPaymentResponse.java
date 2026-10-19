package com.learnflow.backend.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DemoPaymentResponse(
    Long orderId,
    BigDecimal amount,
    String paymentStatus,
    String orderStatus,
    Instant expiresAt,
    String token
) {}
