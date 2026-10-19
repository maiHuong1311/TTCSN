package com.learnflow.backend.order;

import com.learnflow.backend.order.domain.Payment;

public interface PaymentRepository
    extends org.springframework.data.jpa.repository.JpaRepository<Payment, Long>
{
    java.util.Optional<Payment> findByOrderId(Long id);
    java.util.Optional<Payment> findByDemoTokenHash(String hash);
}
