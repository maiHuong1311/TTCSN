package com.learnflow.backend.auth.dto;

import jakarta.validation.constraints.*;

public record VerifyPasswordOtpRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Pattern(regexp = "[0-9]{6}", message = "Mã OTP phải gồm 6 chữ số.") String otp
) {}
