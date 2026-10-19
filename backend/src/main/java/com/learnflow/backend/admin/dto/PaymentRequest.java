package com.learnflow.backend.admin.dto;

import jakarta.validation.constraints.*;

public record PaymentRequest(@NotBlank @Size(max = 255) String transactionId) {}
