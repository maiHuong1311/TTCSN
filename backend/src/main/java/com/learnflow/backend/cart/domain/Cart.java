package com.learnflow.backend.cart.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cart")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    public Long id;

    @Column(name = "user_id")
    public Long userId;

    @Column(name = "created_at")
    public Instant createdAt;

    public Cart() {}
}
