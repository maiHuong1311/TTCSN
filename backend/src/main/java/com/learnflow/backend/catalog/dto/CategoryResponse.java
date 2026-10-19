package com.learnflow.backend.catalog.dto;

public record CategoryResponse(Long categoryId, String categoryName, Long parentId, String description) {}
