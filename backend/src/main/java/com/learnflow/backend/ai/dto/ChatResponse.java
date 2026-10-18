package com.learnflow.backend.ai.dto;

import com.learnflow.backend.catalog.dto.ProductResponse;
import java.util.List;

public record ChatResponse(
    String mode,
    String answer,
    String suggestedSize,
    List<ProductResponse> products
) {}
