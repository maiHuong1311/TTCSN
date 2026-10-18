package com.learnflow.backend.cart.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cart_item")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    public Long id;

    @Column(name = "cart_id")
    public Long cartId;

    @Column(name = "variant_id")
    public Long variantId;

    @Column(name = "quantity")
    public Integer quantity;

    public CartItem() {}
}
