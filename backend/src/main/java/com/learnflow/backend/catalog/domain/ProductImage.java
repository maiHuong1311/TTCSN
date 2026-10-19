package com.learnflow.backend.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_image")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    public Long id;

    @Column(name = "product_id")
    public Long productId;

    @Column(name = "image_url", length = 500)
    public String imageUrl;

    @Column(name = "is_primary")
    public Boolean isPrimary;

    public ProductImage() {}
}
