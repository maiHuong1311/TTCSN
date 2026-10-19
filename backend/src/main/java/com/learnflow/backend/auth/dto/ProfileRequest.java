package com.learnflow.backend.auth.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProfileRequest(
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank
    @Pattern(
        regexp = "(?:0|\\+84)[35789][0-9]{8}",
        message = "Số điện thoại Việt Nam phải gồm 10 số bắt đầu bằng 03, 05, 07, 08, 09 hoặc dùng +84."
    )
    String phoneNumber,
    @DecimalMin("50") @DecimalMax("250") BigDecimal heightCm,
    @DecimalMin("20") @DecimalMax("350") BigDecimal weightKg,
    @Size(max = 100) String preferredStyle
) {}
