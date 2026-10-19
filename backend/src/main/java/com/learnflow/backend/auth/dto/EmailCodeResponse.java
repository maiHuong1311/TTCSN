package com.learnflow.backend.auth.dto;

import java.time.Instant;

public record EmailCodeResponse(String message, Instant expiresAt, Instant resendAt) {}
