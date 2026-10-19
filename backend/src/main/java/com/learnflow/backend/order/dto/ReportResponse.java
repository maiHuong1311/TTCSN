package com.learnflow.backend.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportResponse(
    long orders,
    BigDecimal revenue,
    long pendingOrders,
    List<SellerResponse> bestSellers
) {
    public record SellerResponse(Long productId, String productName, long quantity, BigDecimal revenue) {}
}
