package com.learnflow.backend.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    public Long id;

    @Column(name = "category_name", length = 100)
    public String categoryName;

    @Column(name = "parent_id")
    public Long parentId;

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    public Category() {}
}
