package com.learnflow.backend.admin;

import com.learnflow.backend.admin.dto.*;
import com.learnflow.backend.auth.dto.*;
import com.learnflow.backend.catalog.dto.*;
import com.learnflow.backend.common.web.PageResponse;
import com.learnflow.backend.coupon.dto.*;
import com.learnflow.backend.order.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService s;

    public AdminController(AdminService s) {
        this.s = s;
    }

    @GetMapping("/products")
    public PageResponse<ProductResponse> products(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return s.products(page, size);
    }

    @PostMapping("/products")
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest r) {
        return s.product(null, r);
    }

    @PutMapping("/products/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest r) {
        return s.product(id, r);
    }

    @DeleteMapping("/products/{id}")
    public void deleteProduct(@PathVariable Long id) {
        s.deleteProduct(id);
    }

    @PostMapping("/categories")
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest r) {
        return s.category(null, r);
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest r) {
        return s.category(id, r);
    }

    @DeleteMapping("/categories/{id}")
    public void deleteCategory(@PathVariable Long id) {
        s.deleteCategory(id);
    }

    @GetMapping("/orders")
    public List<OrderResponse> orders() {
        return s.orders();
    }

    @PatchMapping("/orders/{id}/status")
    public OrderResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return s.status(id, r.status());
    }

    @PostMapping("/orders/{id}/payment")
    public OrderResponse payment(@PathVariable Long id, @Valid @RequestBody PaymentRequest r) {
        return s.payment(id, r.transactionId());
    }

    @GetMapping("/reports")
    public ReportResponse report() {
        return s.report();
    }

    @GetMapping("/behavior")
    public List<BehaviorResponse> behavior() {
        return s.behavior();
    }

    @GetMapping("/coupons")
    public List<CouponResponse> coupons() {
        return s.coupons();
    }

    @PostMapping("/coupons")
    public CouponResponse createCoupon(@Valid @RequestBody CouponRequest r) {
        return s.coupon(null, r);
    }

    @PutMapping("/coupons/{id}")
    public CouponResponse updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponRequest r) {
        return s.coupon(id, r);
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return s.users();
    }

    @PatchMapping("/users/{id}/status")
    public UserResponse userStatus(
        Authentication a,
        @PathVariable Long id,
        @Valid @RequestBody UserStatusRequest r
    ) {
        return s.userStatus((Long) a.getPrincipal(), id, r.status());
    }
}
