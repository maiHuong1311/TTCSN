package com.learnflow.backend.order;

import com.learnflow.backend.address.AddressService;
import com.learnflow.backend.auth.AuthService;
import com.learnflow.backend.cart.CartService;
import com.learnflow.backend.catalog.CatalogService;
import com.learnflow.backend.common.web.ApiException;
import com.learnflow.backend.coupon.CouponService;
import com.learnflow.backend.order.domain.*;
import com.learnflow.backend.order.dto.*;
import java.math.*;
import java.time.Clock;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class OrderService {

    private final ShopOrderRepository orders;
    private final OrderItemRepository items;
    private final PaymentRepository payments;
    private final CartService carts;
    private final AddressService addresses;
    private final CatalogService catalog;
    private final CouponService coupons;
    private final AuthService users;
    private final Clock clock;

    public OrderService(
        ShopOrderRepository o,
        OrderItemRepository i,
        PaymentRepository p,
        CartService c,
        AddressService a,
        CatalogService g,
        CouponService d,
        AuthService u,
        Clock k
    ) {
        orders = o;
        items = i;
        payments = p;
        carts = c;
        addresses = a;
        catalog = g;
        coupons = d;
        users = u;
        clock = k;
    }

    public QuoteResponse quote(Long u, String code) {
        var cart = carts.get(u);
        if (cart.items().isEmpty()) throw bad("Cart is empty");
        BigDecimal discount = BigDecimal.ZERO;
        if (code != null && !code.isBlank()) {
            var c = coupons.lock(code);
            discount = coupons.discount(c, cart.subtotal(), orders.countByCouponId(c.id));
        }
        BigDecimal fee =
            cart.subtotal().compareTo(BigDecimal.valueOf(1000000)) >= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(30000);
        return new QuoteResponse(cart.subtotal(), fee, discount, cart.subtotal().add(fee).subtract(discount));
    }

    public OrderResponse checkout(Long u, CheckoutRequest r) {
        users.lockUser(u);
        var address = addresses.require(u, r.addressId());
        var lines = carts.checkoutItems(u);
        if (lines.isEmpty()) throw bad("Cart is empty");
        // Match the admin lock order: products first, then variants, each in ascending ID order.
        lines
            .stream()
            .map(i -> catalog.variant(i.variantId).productId)
            .distinct()
            .sorted()
            .forEach(catalog::lockProduct);
        lines
            .stream()
            .map(i -> i.variantId)
            .sorted()
            .forEach(catalog::lockVariant);
        var quote = quote(u, r.couponCode());
        ShopOrder o = new ShopOrder();
        o.userId = u;
        o.receiverName = address.receiverName;
        o.receiverPhone = address.phoneNumber;
        o.shippingAddress = address.detailAddress + ", " + address.province;
        o.totalAmount = quote.totalAmount();
        o.shippingFee = quote.shippingFee();
        o.discountAmount = quote.discountAmount();
        o.orderStatus = "Pending";
        o.note = r.note();
        o.createdAt = clock.instant();
        if (r.couponCode() != null && !r.couponCode().isBlank()) o.couponId = coupons.lock(r.couponCode()).id;
        orders.save(o);
        for (var line : lines) {
            catalog.takeStock(line.variantId, line.quantity);
            var variant = catalog.variant(line.variantId);
            var product = catalog.product(variant.productId);
            OrderItem i = new OrderItem();
            i.orderId = o.id;
            i.variantId = variant.id;
            i.quantity = line.quantity;
            i.unitPrice = product.salePrice == null ? product.price : product.salePrice;
            items.save(i);
        }
        Payment p = new Payment();
        p.orderId = o.id;
        p.paymentMethod = r.paymentMethod();
        p.paymentStatus = "Pending";
        p.amount = o.totalAmount;
        payments.save(p);
        carts.clear(u);
        return response(o);
    }

    public List<OrderResponse> list(Long u, boolean admin) {
        return (
            admin
                ? orders.findAll(
                      org.springframework.data.domain.Sort.by(
                          org.springframework.data.domain.Sort.Direction.DESC,
                          "createdAt"
                      )
                  )
                : orders.findByUserIdOrderByCreatedAtDesc(u)
        )
            .stream()
            .map(this::response)
            .toList();
    }

    public OrderResponse get(Long u, Long id, boolean admin) {
        ShopOrder o = require(id);
        if (!admin && !o.userId.equals(u)) throw missing();
        return response(o);
    }

    public OrderResponse cancel(Long u, Long id) {
        ShopOrder o = orders.lockById(id).orElseThrow(() -> missing());
        if (!o.userId.equals(u)) throw missing();
        if (!"Pending".equals(o.orderStatus)) throw bad("Only pending orders can be cancelled by customers");
        return transition(o, "Cancelled");
    }

    public OrderResponse status(Long id, String target) {
        return transition(orders.lockById(id).orElseThrow(() -> missing()), target);
    }

    private OrderResponse transition(ShopOrder o, String target) {
        Set<String> allowed = switch (o.orderStatus) {
            case "Pending" -> Set.of("Confirmed", "Cancelled");
            case "Confirmed" -> Set.of("Shipping", "Cancelled");
            case "Shipping" -> Set.of("Delivered");
            default -> Set.of();
        };
        if (!allowed.contains(target)) throw bad("Invalid order status transition");
        Payment p = payments.findByOrderId(o.id).orElseThrow();
        if (
            "Shipping".equals(target) && !"COD".equals(p.paymentMethod) && !"Paid".equals(p.paymentStatus)
        ) throw bad("Online payment must be confirmed before shipping");
        if ("Cancelled".equals(target)) {
            items
                .findByOrderId(o.id)
                .stream()
                .sorted(Comparator.comparing(i -> i.variantId))
                .forEach(i -> catalog.restoreStock(i.variantId, i.quantity));
            p.paymentStatus = "Paid".equals(p.paymentStatus) ? "Refunded" : "Failed";
        }
        if ("Delivered".equals(target) && "COD".equals(p.paymentMethod)) {
            p.paymentStatus = "Paid";
            p.paidAt = clock.instant();
        }
        o.orderStatus = target;
        return response(o);
    }

    public OrderResponse confirmPayment(Long id, String transactionId) {
        ShopOrder o = orders.lockById(id).orElseThrow(() -> missing());
        Payment p = payments.findByOrderId(id).orElseThrow();
        if (
            "Cancelled".equals(o.orderStatus) ||
            !"Pending".equals(p.paymentStatus) ||
            ("COD".equals(p.paymentMethod) || "DemoQR".equals(p.paymentMethod))
        ) throw bad("Payment cannot be confirmed");
        if (transactionId == null || transactionId.isBlank() || transactionId.length() > 255) throw bad(
            "Transaction reference is required"
        );
        p.transactionId = transactionId;
        p.paymentStatus = "Paid";
        p.paidAt = clock.instant();
        return response(o);
    }

    public boolean purchased(Long u, Long productId) {
        return orders
            .findByUserIdOrderByCreatedAtDesc(u)
            .stream()
            .filter(o -> "Delivered".equals(o.orderStatus))
            .flatMap(o -> items.findByOrderId(o.id).stream())
            .anyMatch(i -> catalog.variant(i.variantId).productId.equals(productId));
    }

    public ReportResponse report() {
        var all = orders.findAll();
        BigDecimal revenue = BigDecimal.ZERO;
        Map<Long, ReportResponse.SellerResponse> sellers = new HashMap<>();
        for (var o : all)
            if ("Delivered".equals(o.orderStatus)) {
                revenue = revenue.add(o.totalAmount);
                for (var i : items.findByOrderId(o.id)) {
                    var p = catalog.product(catalog.variant(i.variantId).productId);
                    var old = sellers.getOrDefault(
                        p.id,
                        new ReportResponse.SellerResponse(p.id, p.productName, 0, BigDecimal.ZERO)
                    );
                    sellers.put(
                        p.id,
                        new ReportResponse.SellerResponse(
                            p.id,
                            p.productName,
                            old.quantity() + i.quantity,
                            old.revenue().add(i.unitPrice.multiply(BigDecimal.valueOf(i.quantity)))
                        )
                    );
                }
            }
        return new ReportResponse(
            all.size(),
            revenue,
            all
                .stream()
                .filter(o -> "Pending".equals(o.orderStatus))
                .count(),
            sellers
                .values()
                .stream()
                .sorted(Comparator.comparingLong(ReportResponse.SellerResponse::quantity).reversed())
                .limit(10)
                .toList()
        );
    }

    private ShopOrder require(Long id) {
        return orders.findById(id).orElseThrow(() -> missing());
    }

    private OrderResponse response(ShopOrder o) {
        var p = payments.findByOrderId(o.id).orElseThrow();
        return new OrderResponse(
            o.id,
            o.userId,
            o.receiverName,
            o.receiverPhone,
            o.shippingAddress,
            o.totalAmount,
            o.shippingFee,
            o.discountAmount,
            o.orderStatus,
            o.note,
            o.createdAt,
            items
                .findByOrderId(o.id)
                .stream()
                .map(i -> {
                    var v = catalog.variant(i.variantId);
                    var product = catalog.product(v.productId);
                    return new OrderResponse.ItemResponse(
                        i.id,
                        v.id,
                        product.id,
                        product.productName,
                        v.size,
                        v.color,
                        i.quantity,
                        i.unitPrice
                    );
                })
                .toList(),
            new OrderResponse.PaymentResponse(
                p.id,
                p.paymentMethod,
                p.paymentStatus,
                p.amount,
                p.transactionId,
                p.paidAt
            )
        );
    }

    private ApiException bad(String m) {
        return new ApiException(HttpStatus.BAD_REQUEST, m);
    }

    private ApiException missing() {
        return new ApiException(HttpStatus.NOT_FOUND, "Order not found");
    }
}
