package com.learnflow.backend.order.dto;

import java.math.BigDecimal;

public record QuoteResponse(
    BigDecimal subtotal,
    BigDecimal shippingFee,
    BigDecimal discountAmount,
    BigDecimal totalAmount
) {}
