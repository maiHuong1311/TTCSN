package com.learnflow.backend.cart.dto;

import jakarta.validation.constraints.*;

public record CartItemRequest(@NotNull Long variantId, @Min(1) @Max(100) int quantity, boolean trackingConsent) {}
