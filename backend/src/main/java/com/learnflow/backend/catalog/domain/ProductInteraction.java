package com.learnflow.backend.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_interaction")
public class ProductInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "user_id")
    public Long userId;

    @Column(name = "product_id")
    public Long productId;

    @Column(name = "event_type", length = 20)
    public String eventType;

    @Column(name = "query_text", length = 100)
    public String queryText;

    @Column(name = "created_at")
    public Instant createdAt;

    public ProductInteraction() {}
}
