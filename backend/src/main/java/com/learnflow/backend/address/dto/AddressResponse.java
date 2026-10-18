package com.learnflow.backend.address.dto;

public record AddressResponse(
    Long addressId,
    String receiverName,
    String phoneNumber,
    String province,
    String detailAddress,
    boolean isDefault
) {}
