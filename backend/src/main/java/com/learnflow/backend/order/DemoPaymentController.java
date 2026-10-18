package com.learnflow.backend.order;

import com.learnflow.backend.order.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class DemoPaymentController {

    private final DemoPaymentService service;

    public DemoPaymentController(DemoPaymentService service) {
        this.service = service;
    }

    @PostMapping("/api/orders/{id}/demo-payment")
    public DemoPaymentResponse create(Authentication auth, @PathVariable Long id) {
        return service.create((Long) auth.getPrincipal(), id);
    }

    @PostMapping("/api/demo-payments/view")
    public DemoPaymentResponse view(@Valid @RequestBody DemoPaymentRequest request) {
        return service.view(request.token());
    }

    @PostMapping("/api/demo-payments/complete")
    public DemoPaymentResponse complete(@Valid @RequestBody DemoPaymentRequest request) {
        return service.complete(request.token(), request.outcome());
    }
}
