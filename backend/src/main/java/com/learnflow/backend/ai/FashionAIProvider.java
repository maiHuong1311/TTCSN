package com.learnflow.backend.ai;

import com.learnflow.backend.ai.dto.ChatRequest;
import com.learnflow.backend.catalog.dto.ProductResponse;
import java.util.List;

public interface FashionAIProvider {
    boolean configured();
    Selection advise(ChatRequest request, List<ProductResponse> catalog);

    record Selection(List<Long> productIds, String suggestedSize) {}
}
