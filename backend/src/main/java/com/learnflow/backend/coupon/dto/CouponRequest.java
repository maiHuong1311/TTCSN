package com.learnflow.backend.coupon.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public record CouponRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,50}") String couponCode,
    @NotBlank @Pattern(regexp = "Percentage|Fixed") String discountType,
    @NotNull @DecimalMin("0.01") BigDecimal discountValue,
    @NotNull @DecimalMin("0") BigDecimal minOrderValue,
    @Min(0) int usageLimit,
    @NotNull Instant startDate,
    @NotNull Instant endDate
) {}
