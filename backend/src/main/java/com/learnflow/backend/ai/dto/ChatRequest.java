package com.learnflow.backend.ai.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ChatRequest(
    @NotBlank @Size(max = 2000) String message,
    Long orderId,
    @DecimalMin("50") @DecimalMax("250") BigDecimal heightCm,
    @DecimalMin("20") @DecimalMax("350") BigDecimal weightKg
) {}
