package com.learnflow.backend.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    public Long id;

    @Column(name = "order_id")
    public Long orderId;

    @Column(name = "variant_id")
    public Long variantId;

    @Column(name = "quantity")
    public Integer quantity;

    @Column(name = "unit_price", precision = 12, scale = 2)
    public BigDecimal unitPrice;

    public OrderItem() {}
}
