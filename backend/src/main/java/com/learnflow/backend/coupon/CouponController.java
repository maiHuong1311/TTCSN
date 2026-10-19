package com.learnflow.backend.coupon;

import com.learnflow.backend.coupon.dto.CouponResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {
    private final CouponService service;

    public CouponController(CouponService service) {
        this.service = service;
    }

    @GetMapping
    public List<CouponResponse> available() {
        return service.available();
    }
}
