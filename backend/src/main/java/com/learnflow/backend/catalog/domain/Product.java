package com.learnflow.backend.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    public Long id;

    @Column(name = "category_id")
    public Long categoryId;

    @Column(name = "product_name", length = 255)
    public String productName;

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Column(name = "brand", length = 100)
    public String brand;

    @Column(name = "material", length = 100)
    public String material;

    @Column(name = "gender", length = 20)
    public String gender;

    @Column(name = "price", precision = 12, scale = 2)
    public BigDecimal price;

    @Column(name = "sale_price", precision = 12, scale = 2)
    public BigDecimal salePrice;

    @Column(name = "status", length = 20)
    public String status;

    @Column(name = "size_chart_code", length = 32)
    public String sizeChartCode;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    public Product() {}
}
