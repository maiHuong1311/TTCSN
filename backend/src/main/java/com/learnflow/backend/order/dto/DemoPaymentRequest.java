package com.learnflow.backend.order.dto;

import jakarta.validation.constraints.*;

public record DemoPaymentRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Mã thanh toán không hợp lệ.") String token,
    @Pattern(regexp = "Paid|Failed", message = "Kết quả thanh toán không hợp lệ.") String outcome
) {}
