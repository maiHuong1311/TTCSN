package com.learnflow.backend.auth.dto;

public record PasswordOtpVerificationResponse(String token, int expiresInSeconds) {}
