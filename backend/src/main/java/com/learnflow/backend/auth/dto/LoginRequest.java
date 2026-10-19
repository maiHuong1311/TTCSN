package com.learnflow.backend.auth.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
    @com.fasterxml.jackson.annotation.JsonAlias("email") @NotBlank @Size(max = 255) String identifier,
    @NotBlank @Size(max = 72) String password
) {}
