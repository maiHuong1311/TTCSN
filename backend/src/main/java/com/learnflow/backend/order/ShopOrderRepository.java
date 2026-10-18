package com.learnflow.backend.order;

import com.learnflow.backend.order.domain.ShopOrder;

public interface ShopOrderRepository
    extends org.springframework.data.jpa.repository.JpaRepository<ShopOrder, Long>
{
    java.util.List<ShopOrder> findByUserIdOrderByCreatedAtDesc(Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from ShopOrder o where o.id=:id")
    java.util.Optional<ShopOrder> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    long countByCouponId(Long id);
}
