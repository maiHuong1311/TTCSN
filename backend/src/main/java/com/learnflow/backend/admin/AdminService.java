package com.learnflow.backend.admin;

import com.learnflow.backend.auth.*;
import com.learnflow.backend.auth.dto.*;
import com.learnflow.backend.catalog.*;
import com.learnflow.backend.catalog.dto.*;
import com.learnflow.backend.common.web.PageResponse;
import com.learnflow.backend.coupon.*;
import com.learnflow.backend.coupon.dto.*;
import com.learnflow.backend.order.*;
import com.learnflow.backend.order.dto.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminService {

    private final CatalogService catalog;
    private final OrderService orders;
    private final CouponService coupons;
    private final AuthService users;

    public AdminService(CatalogService c, OrderService o, CouponService p, AuthService u) {
        catalog = c;
        orders = o;
        coupons = p;
        users = u;
    }

    public PageResponse<ProductResponse> products(int page, int size) {
        return catalog.list("", null, null, page, size, true);
    }

    public ProductResponse product(Long id, ProductRequest r) {
        return catalog.save(id, r);
    }

    public void deleteProduct(Long id) {
        catalog.deactivate(id);
    }

    public CategoryResponse category(Long id, CategoryRequest r) {
        return catalog.saveCategory(id, r);
    }

    public void deleteCategory(Long id) {
        catalog.deleteCategory(id);
    }

    public List<OrderResponse> orders() {
        return orders.list(null, true);
    }

    public OrderResponse status(Long id, String s) {
        return orders.status(id, s);
    }

    public OrderResponse payment(Long id, String tx) {
        return orders.confirmPayment(id, tx);
    }

    public ReportResponse report() {
        return orders.report();
    }

    public List<BehaviorResponse> behavior() {
        return catalog.behavior();
    }

    public List<CouponResponse> coupons() {
        return coupons.list();
    }

    public CouponResponse coupon(Long id, CouponRequest r) {
        return coupons.save(id, r);
    }

    public List<UserResponse> users() {
        return users.users();
    }

    public UserResponse userStatus(Long actor, Long id, String s) {
        return users.status(actor, id, s);
    }
}
