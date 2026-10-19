package com.learnflow.backend.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
    @NotNull Long categoryId,
    @NotBlank @Size(max = 255) String productName,
    @Size(max = 20000) String description,
    @Size(max = 100) String brand,
    @Size(max = 100) String material,
    @NotBlank @Pattern(regexp = "Male|Female|Unisex") String gender,
    @NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") BigDecimal price,
    @DecimalMin("0") BigDecimal salePrice,
    @NotBlank @Pattern(regexp = "Active|Inactive") String status,
    @NotEmpty @Size(max = 100) List<@Valid VariantRequest> variants,
    @Size(max = 20)
    List<@Pattern(regexp = "/assets/[a-zA-Z0-9._/-]+|/api/images/[a-f0-9-]+\\.(png|jpg)|https://[^\\s<>]+") @Size(max = 500) String> images
) {
    public record VariantRequest(
        Long variantId,
        @Size(max = 10) String size,
        @NotBlank @Size(max = 50) String color,
        @NotNull @Min(0) Integer stockQuantity,
        @NotBlank @Size(max = 100) String sku,
        @Pattern(regexp = "/assets/[a-zA-Z0-9._/-]+|/api/images/[a-f0-9-]+\\.(png|jpg)|https://[^\\s<>]+") @Size(max = 500) String imageUrl
    ) {}
}
