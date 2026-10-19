package com.learnflow.backend.review;

import com.learnflow.backend.review.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {

    private final ReviewService s;

    public ReviewController(ReviewService s) {
        this.s = s;
    }

    @GetMapping
    public List<ReviewResponse> list(@PathVariable Long productId) {
        return s.list(productId);
    }

    @PostMapping
    public ReviewResponse create(
        Authentication a,
        @PathVariable Long productId,
        @Valid @RequestBody ReviewRequest r
    ) {
        return s.create((Long) a.getPrincipal(), productId, r);
    }

    @GetMapping("/eligibility")
    public ReviewEligibilityResponse eligibility(Authentication a, @PathVariable Long productId) {
        return s.eligibility((Long) a.getPrincipal(), productId);
    }
}
