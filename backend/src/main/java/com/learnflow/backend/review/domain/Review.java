package com.learnflow.backend.review.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    public Long id;

    @Column(name = "user_id")
    public Long userId;

    @Column(name = "product_id")
    public Long productId;

    @Column(name = "rating")
    public Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    public String comment;

    @Column(name = "image_url", length = 500)
    public String imageUrl;

    @Column(name = "created_at")
    public Instant createdAt;

    public Review() {}
}
