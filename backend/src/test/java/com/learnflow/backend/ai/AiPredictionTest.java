package com.learnflow.backend.ai;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AiPredictionTest {
    @Test void acceptsContractResponse() { assertEquals("CKD_RISK", new AiPrediction("CKD_RISK", new BigDecimal("0.88"), "rf-v1").outcome()); }
    @Test void rejectsUnsupportedOutcome() { assertThrows(IllegalArgumentException.class, () -> new AiPrediction("STAGE_3", BigDecimal.ONE, "v1")); }
    @Test void rejectsRiskOutsideProbabilityRange() { assertThrows(IllegalArgumentException.class, () -> new AiPrediction("NORMAL", new BigDecimal("1.1"), "v1")); }
}
