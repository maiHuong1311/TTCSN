package com.learnflow.backend.address.dto;

import jakarta.validation.constraints.*;

public record AddressRequest(
    @NotBlank @Size(max = 100) String receiverName,
    @NotBlank
    @Pattern(
        regexp = "(?:0|\\+84)[35789][0-9]{8}",
        message = "Số điện thoại Việt Nam phải gồm 10 số bắt đầu bằng 03, 05, 07, 08, 09 hoặc dùng +84."
    )
    String phoneNumber,
    @NotBlank @Size(max = 100) String province,
    @NotBlank @Size(max = 255) String detailAddress,
    boolean isDefault
) {}
