package com.learnflow.backend.ai.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ChatRequest(
    @NotBlank @Size(max = 2000) String message,
    @Positive Long orderId,
    @DecimalMin("50") @DecimalMax("250") BigDecimal heightCm,
    @DecimalMin("20") @DecimalMax("350") BigDecimal weightKg,
    @Positive Long productId,
    @Pattern(regexp = "SIZE_AO_NU|SIZE_QUAN_NU|SIZE_CHAN_VAY|SIZE_DAM")
    String sizeChartCode,
    @DecimalMin("30") @DecimalMax("200") BigDecimal bustCm,
    @DecimalMin("30") @DecimalMax("200") BigDecimal waistCm,
    @DecimalMin("30") @DecimalMax("220") BigDecimal hipCm,
    @DecimalMin("20") @DecimalMax("80") BigDecimal shoulderCm,
    @Size(max = 50) String bodyShape,
    boolean allowExternalAi
) {
    public ChatRequest(
        String message,
        Long orderId,
        BigDecimal heightCm,
        BigDecimal weightKg
    ) {
        this(
            message,
            orderId,
            heightCm,
            weightKg,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false
        );
    }
}
