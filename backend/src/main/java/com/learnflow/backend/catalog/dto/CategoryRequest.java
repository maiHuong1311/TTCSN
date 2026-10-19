package com.learnflow.backend.catalog.dto;

import jakarta.validation.constraints.*;

public record CategoryRequest(
    @NotBlank @Size(max = 100) String categoryName,
    Long parentId,
    @Size(max = 5000) String description
) {}
