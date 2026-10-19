package com.learnflow.backend.order.dto;

import jakarta.validation.constraints.*;

public record StatusRequest(
    @NotBlank @Pattern(regexp = "Confirmed|Shipping|Delivered|Cancelled") String status
) {}
