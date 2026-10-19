package com.learnflow.backend.prediction.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PredictionResponse(
    Long id, String patientCode, String status, String outcome, BigDecimal riskScore,
    String modelVersion, String failureReason, Instant createdAt
) {}
