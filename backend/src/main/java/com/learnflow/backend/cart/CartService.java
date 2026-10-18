package com.learnflow.backend.cart;

import com.learnflow.backend.auth.AuthService;
import com.learnflow.backend.cart.domain.*;
import com.learnflow.backend.cart.dto.*;
import com.learnflow.backend.catalog.CatalogService;
import com.learnflow.backend.common.web.ApiException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartService {

    private final CartRepository carts;
    private final CartItemRepository items;
    private final CatalogService catalog;
    private final AuthService users;
    private final Clock clock;

    public CartService(CartRepository c, CartItemRepository i, CatalogService p, AuthService u, Clock k) {
        carts = c;
        items = i;
        catalog = p;
        users = u;
        clock = k;
    }

    public Cart cart(Long u) {
        users.lockUser(u);
        return carts.findByUserId(u).orElseGet(() -> {
            Cart c = new Cart();
            c.userId = u;
            c.createdAt = clock.instant();
            return carts.save(c);
        });
    }

    public CartResponse get(Long u) {
        Cart c = cart(u);
        var lines = items
            .findByCartIdOrderByVariantId(c.id)
            .stream()
            .map(i -> {
                var v = catalog.variant(i.variantId);
                var p = catalog.product(v.productId);
                var images = catalog.snapshot(p.id).images();
                return new CartResponse.ItemResponse(
                    i.id,
                    v.id,
                    p.id,
                    p.productName,
                    v.size,
                    v.color,
                    i.quantity,
                    "Active".equals(p.status) ? v.stockQuantity : 0,
                    p.salePrice == null ? p.price : p.salePrice,
                    v.imageUrl != null
                        ? v.imageUrl
                        : images.isEmpty()
                          ? "/assets/photos/product-1.jpg"
                          : images.get(0).imageUrl()
                );
            })
            .toList();
        return new CartResponse(
            c.id,
            lines,
            lines
                .stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    public CartResponse put(Long u, CartItemRequest r, boolean increment) {
        Cart c = cart(u);
        var v = catalog.variant(r.variantId());
        catalog.get(v.productId);
        CartItem i = items
            .findByCartIdOrderByVariantId(c.id)
            .stream()
            .filter(x -> x.variantId.equals(v.id))
            .findFirst()
            .orElseGet(() -> {
                CartItem n = new CartItem();
                n.cartId = c.id;
                n.variantId = v.id;
                n.quantity = 0;
                return n;
            });
        int quantity = r.quantity() + (increment ? i.quantity : 0);
        if (quantity > 100 || quantity > v.stockQuantity) throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "Insufficient stock or quantity exceeds 100"
        );
        i.quantity = quantity;
        items.save(i);
        return get(u);
    }

    public CartResponse remove(Long u, Long id) {
        Cart c = cart(u);
        CartItem i = items
            .findById(id)
            .filter(x -> x.cartId.equals(c.id))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cart item not found"));
        items.delete(i);
        items.flush();
        return get(u);
    }

    public List<CartItem> checkoutItems(Long u) {
        return items.findByCartIdOrderByVariantId(cart(u).id);
    }

    public void clear(Long u) {
        items.deleteByCartId(cart(u).id);
    }
}
