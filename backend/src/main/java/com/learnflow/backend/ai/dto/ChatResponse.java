package com.learnflow.backend.ai.dto;

import com.learnflow.backend.catalog.dto.ProductResponse;
import java.util.List;

public record ChatResponse(
    String mode,
    String answer,
    String suggestedSize,
    List<ProductResponse> products,
    com.learnflow.backend.store.SizeGuideService.Advice sizeAdvice
) {
    public ChatResponse(
        String mode,
        String answer,
        String suggestedSize,
        List<ProductResponse> products
    ) {
        this(mode, answer, suggestedSize, products, null);
    }
}
