package com.learnflow.backend.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_variant")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    public Long id;

    @Column(name = "product_id")
    public Long productId;

    @Column(name = "size", length = 10)
    public String size;

    @Column(name = "color", length = 50)
    public String color;

    @Column(name = "stock_quantity")
    public Integer stockQuantity;

    @Column(name = "sku", length = 100)
    public String sku;

    @Column(name = "image_url", length = 500)
    public String imageUrl;

    public ProductVariant() {}
}
