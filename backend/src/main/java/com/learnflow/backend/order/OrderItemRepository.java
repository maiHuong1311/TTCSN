package com.learnflow.backend.order;

import com.learnflow.backend.order.domain.OrderItem;

public interface OrderItemRepository
    extends org.springframework.data.jpa.repository.JpaRepository<OrderItem, Long>
{
    java.util.List<OrderItem> findByOrderId(Long id);
}
