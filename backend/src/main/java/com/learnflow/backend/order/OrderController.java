package com.learnflow.backend.order;

import com.learnflow.backend.order.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService s;

    public OrderController(OrderService s) {
        this.s = s;
    }

    @GetMapping
    public List<OrderResponse> list(Authentication a) {
        return s.list((Long) a.getPrincipal(), false);
    }

    @GetMapping("/{id}")
    public OrderResponse get(Authentication a, @PathVariable Long id) {
        return s.get((Long) a.getPrincipal(), id, false);
    }

    @PostMapping("/quote")
    public QuoteResponse quote(Authentication a, @Valid @RequestBody QuoteRequest r) {
        return s.quote((Long) a.getPrincipal(), r.couponCode());
    }

    @PostMapping
    public OrderResponse create(Authentication a, @Valid @RequestBody CheckoutRequest r) {
        return s.checkout((Long) a.getPrincipal(), r);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(Authentication a, @PathVariable Long id) {
        return s.cancel((Long) a.getPrincipal(), id);
    }
}
