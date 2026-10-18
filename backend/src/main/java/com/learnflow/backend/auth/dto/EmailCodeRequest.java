package com.learnflow.backend.auth.dto;

import jakarta.validation.constraints.*;

public record EmailCodeRequest(@NotBlank @Email @Size(max = 255) String email) {}
