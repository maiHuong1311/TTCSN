package com.learnflow.backend.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponResponse(
    Long couponId,
    String couponCode,
    String discountType,
    BigDecimal discountValue,
    BigDecimal minOrderValue,
    int usageLimit,
    Instant startDate,
    Instant endDate
) {}
