package com.learnflow.backend.order.dto;

import jakarta.validation.constraints.*;

public record CheckoutRequest(
    @NotNull Long addressId,
    @NotBlank @Pattern(regexp = "COD|VietQR|EWallet|DemoQR") String paymentMethod,
    @Size(max = 50) String couponCode,
    @Size(max = 2000) String note,
    boolean trackingConsent
) {}
