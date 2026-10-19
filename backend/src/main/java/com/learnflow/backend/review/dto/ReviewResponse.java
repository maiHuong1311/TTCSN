package com.learnflow.backend.review.dto;

import java.time.Instant;

public record ReviewResponse(
    Long reviewId,
    Long userId,
    String fullName,
    Long productId,
    int rating,
    String comment,
    String imageUrl,
    Instant createdAt
) {}
