package com.learnflow.backend.ai;

import java.math.BigDecimal;

public record AiPrediction(String outcome, BigDecimal riskScore, String modelVersion) {
    public AiPrediction {
        if (!"CKD_RISK".equals(outcome) && !"NORMAL".equals(outcome)) throw new IllegalArgumentException("AI outcome must be CKD_RISK or NORMAL");
        if (riskScore == null || riskScore.compareTo(BigDecimal.ZERO) < 0 || riskScore.compareTo(BigDecimal.ONE) > 0) throw new IllegalArgumentException("AI riskScore must be between 0 and 1");
    }
}
