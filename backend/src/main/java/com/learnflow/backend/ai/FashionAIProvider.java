package com.learnflow.backend.ai;

import com.learnflow.backend.ai.dto.ChatRequest;
import com.learnflow.backend.catalog.dto.ProductResponse;
import java.util.List;

public interface FashionAIProvider {
  boolean configured();

  default com.learnflow.backend.ai.dto.AiStatusResponse status() {
    return new com.learnflow.backend.ai.dto.AiStatusResponse(
      configured(),
      configured() ? "API" : "Chưa kết nối"
    );
  }

  Selection advise(ChatRequest request, List<ProductResponse> catalog);

  record Selection(
    List<Long> productIds,
    String suggestedSize,
    String occasion,
    String mode,
    java.math.BigDecimal maxPrice,
    String answer
  ) {
    public Selection(List<Long> productIds, String suggestedSize) {
      this(productIds, suggestedSize, null, "API", null, null);
    }

    public Selection(
      List<Long> productIds,
      String suggestedSize,
      String occasion,
      String mode,
      java.math.BigDecimal maxPrice
    ) {
      this(productIds, suggestedSize, occasion, mode, maxPrice, null);
    }
  }
}
