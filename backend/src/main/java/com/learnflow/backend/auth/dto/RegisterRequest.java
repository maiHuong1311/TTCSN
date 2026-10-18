package com.learnflow.backend.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Pattern(regexp = "[a-zA-Z0-9_]{3,50}") String username,
    @NotBlank
    @Pattern(
        regexp = "(?:0|\\+84)[35789][0-9]{8}",
        message = "Số điện thoại Việt Nam phải gồm 10 số bắt đầu bằng 03, 05, 07, 08, 09 hoặc dùng +84."
    )
    String phoneNumber,
    @NotBlank
    @Pattern(regexp = "[0-9]{6}", message = "Mã xác minh phải gồm 6 chữ số.")
    String verificationCode
) {}
