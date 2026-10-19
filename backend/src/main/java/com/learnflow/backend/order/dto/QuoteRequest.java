package com.learnflow.backend.order.dto;

import jakarta.validation.constraints.*;

public record QuoteRequest(@Size(max = 50) String couponCode) {}
