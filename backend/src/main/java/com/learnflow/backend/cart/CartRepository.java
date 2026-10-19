package com.learnflow.backend.cart;

import com.learnflow.backend.cart.domain.Cart;

public interface CartRepository extends org.springframework.data.jpa.repository.JpaRepository<Cart, Long> {
    java.util.Optional<Cart> findByUserId(Long id);
}
