package com.learnflow.backend.review.dto;

public record ReviewEligibilityResponse(
    boolean canReview,
    boolean deliveredPurchase,
    boolean alreadyReviewed
) {}
