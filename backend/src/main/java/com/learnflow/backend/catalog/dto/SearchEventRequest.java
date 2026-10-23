package com.learnflow.backend.catalog.dto;
import jakarta.validation.constraints.*;
public record SearchEventRequest(@NotBlank @Size(max=100) String query, @AssertTrue boolean consent) {}
