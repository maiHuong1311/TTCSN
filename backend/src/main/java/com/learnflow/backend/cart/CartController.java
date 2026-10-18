package com.learnflow.backend.cart;

import com.learnflow.backend.cart.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts/current")
public class CartController {

    private final CartService s;

    public CartController(CartService s) {
        this.s = s;
    }

    @GetMapping
    public CartResponse get(Authentication a) {
        return s.get((Long) a.getPrincipal());
    }

    @PostMapping("/items")
    public CartResponse add(Authentication a, @Valid @RequestBody CartItemRequest r) {
        return s.put((Long) a.getPrincipal(), r, true);
    }

    @PutMapping("/items")
    public CartResponse update(Authentication a, @Valid @RequestBody CartItemRequest r) {
        return s.put((Long) a.getPrincipal(), r, false);
    }

    @DeleteMapping("/items/{id}")
    public CartResponse delete(Authentication a, @PathVariable Long id) {
        return s.remove((Long) a.getPrincipal(), id);
    }
}
