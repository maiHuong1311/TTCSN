package com.learnflow.backend.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    public Long id;

    @Column(name = "order_id")
    public Long orderId;

    @Column(name = "payment_method", length = 20)
    public String paymentMethod;

    @Column(name = "payment_status", length = 20)
    public String paymentStatus;

    @Column(name = "amount", precision = 12, scale = 2)
    public BigDecimal amount;

    @Column(name = "transaction_id", length = 255)
    public String transactionId;

    @Column(name = "paid_at")
    public Instant paidAt;

    @Column(name = "demo_token_hash", length = 64)
    public String demoTokenHash;

    @Column(name = "demo_expires_at")
    public Instant demoExpiresAt;

    public Payment() {}
}
