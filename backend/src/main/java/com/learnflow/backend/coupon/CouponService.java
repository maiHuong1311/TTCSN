package com.learnflow.backend.coupon;

import com.learnflow.backend.common.web.ApiException;
import com.learnflow.backend.coupon.domain.Coupon;
import com.learnflow.backend.coupon.dto.*;
import java.math.*;
import java.time.Clock;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CouponService {

    private final CouponRepository repository;
    private final Clock clock;

    public CouponService(CouponRepository r, Clock k) {
        repository = r;
        clock = k;
    }

    public List<CouponResponse> list() {
        return repository.findAll().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> available(Long userId) {
        return repository.findAvailable(clock.instant(), userId).stream().map(this::dto).toList();
    }

    public CouponResponse save(Long id, CouponRequest r) {
        if (
            !r.endDate().isAfter(r.startDate()) ||
            ("Percentage".equals(r.discountType()) &&
                r.discountValue().compareTo(BigDecimal.valueOf(100)) > 0)
        ) throw bad("Invalid coupon dates or percentage");
        Coupon c =
            id == null ? new Coupon() : repository.findById(id).orElseThrow(() -> bad("Coupon not found"));
        c.couponCode = r.couponCode().toUpperCase(Locale.ROOT);
        c.discountType = r.discountType();
        c.discountValue = r.discountValue();
        c.minOrderValue = r.minOrderValue();
        c.usageLimit = r.usageLimit();
        c.startDate = r.startDate();
        c.endDate = r.endDate();
        return dto(repository.save(c));
    }

    public Coupon lock(String code) {
        Coupon c = repository.lockByCode(code.trim()).orElseThrow(() -> bad("Coupon not found"));
        if (clock.instant().isBefore(c.startDate) || !clock.instant().isBefore(c.endDate)) throw bad(
            "Coupon is not active"
        );
        return c;
    }

    public BigDecimal discount(Coupon c, BigDecimal subtotal, long uses) {
        if (uses >= c.usageLimit) throw bad("Coupon usage limit reached");
        if (subtotal.compareTo(c.minOrderValue) < 0) throw bad("Order does not meet coupon minimum");
        BigDecimal value = "Percentage".equals(c.discountType)
            ? subtotal.multiply(c.discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            : c.discountValue;
        return value.min(subtotal);
    }

    private CouponResponse dto(Coupon c) {
        return new CouponResponse(
            c.id,
            c.couponCode,
            c.discountType,
            c.discountValue,
            c.minOrderValue,
            c.usageLimit,
            c.startDate,
            c.endDate
        );
    }

    private ApiException bad(String m) {
        return new ApiException(HttpStatus.BAD_REQUEST, m);
    }
}
