package com.learnflow.backend.review.dto;

import jakarta.validation.constraints.*;

public record ReviewRequest(
    @Min(1) @Max(5) int rating,
    @NotBlank @Size(max = 5000) String comment,
    @Pattern(regexp = "https://[^\\s<>]+|/assets/[a-zA-Z0-9._/-]+") @Size(max = 500) String imageUrl
) {}
