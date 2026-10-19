package com.learnflow.backend.auth.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record UserResponse(
    Long userId,
    String email,
    String fullName,
    String username,
    String phoneNumber,
    String role,
    String status,
    Instant createdAt,
    BigDecimal heightCm,
    BigDecimal weightKg,
    String preferredStyle
) {}
