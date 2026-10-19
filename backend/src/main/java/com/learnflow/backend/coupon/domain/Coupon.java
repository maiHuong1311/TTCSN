package com.learnflow.backend.coupon.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    public Long id;

    @Column(name = "coupon_code", length = 50)
    public String couponCode;

    @Column(name = "discount_type", length = 20)
    public String discountType;

    @Column(name = "discount_value", precision = 12, scale = 2)
    public BigDecimal discountValue;

    @Column(name = "min_order_value", precision = 12, scale = 2)
    public BigDecimal minOrderValue;

    @Column(name = "usage_limit")
    public Integer usageLimit;

    @Column(name = "start_date")
    public Instant startDate;

    @Column(name = "end_date")
    public Instant endDate;

    public Coupon() {}
}
