package com.learnflow.backend.cart;

import com.learnflow.backend.cart.domain.CartItem;

public interface CartItemRepository
    extends org.springframework.data.jpa.repository.JpaRepository<CartItem, Long>
{
    java.util.List<CartItem> findByCartIdOrderByVariantId(Long id);
    void deleteByCartId(Long id);
}
