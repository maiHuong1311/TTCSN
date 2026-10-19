package com.learnflow.backend.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shop_order")
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    public Long id;

    @Column(name = "user_id")
    public Long userId;

    @Column(name = "receiver_name", length = 100)
    public String receiverName;

    @Column(name = "receiver_phone", length = 15)
    public String receiverPhone;

    @Column(name = "shipping_address", length = 500)
    public String shippingAddress;

    @Column(name = "total_amount", precision = 12, scale = 2)
    public BigDecimal totalAmount;

    @Column(name = "shipping_fee", precision = 12, scale = 2)
    public BigDecimal shippingFee;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    public BigDecimal discountAmount;

    @Column(name = "order_status", length = 20)
    public String orderStatus;

    @Column(name = "note", columnDefinition = "TEXT")
    public String note;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "coupon_id")
    public Long couponId;

    public ShopOrder() {}
}
