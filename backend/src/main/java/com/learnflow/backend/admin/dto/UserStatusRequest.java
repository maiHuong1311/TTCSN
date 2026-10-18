package com.learnflow.backend.admin.dto;

import jakarta.validation.constraints.*;

public record UserStatusRequest(@NotBlank @Pattern(regexp = "Active|Blocked") String status) {}
