package com.learnflow.backend.coupon;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learnflow.backend.common.web.ApiException;
import com.learnflow.backend.coupon.domain.Coupon;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;

class CouponServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-17T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void percentageRoundingAndRedemptionBoundaries() {
        var s = new CouponService(mock(CouponRepository.class), clock);
        Coupon c = new Coupon();
        c.discountType = "Percentage";
        c.discountValue = new BigDecimal("10");
        c.minOrderValue = new BigDecimal("300000");
        c.usageLimit = 1;
        assertEquals(new BigDecimal("39000.00"), s.discount(c, new BigDecimal("390000"), 0));
        assertThrows(ApiException.class, () -> s.discount(c, new BigDecimal("390000"), 1));
        assertThrows(ApiException.class, () -> s.discount(c, new BigDecimal("100"), 0));
        c.discountType = "Fixed";
        c.discountValue = new BigDecimal("900000");
        assertEquals(new BigDecimal("390000"), s.discount(c, new BigDecimal("390000"), 0));
    }

    @Test
    void expirationUsesInjectedClock() {
        var repo = mock(CouponRepository.class);
        var s = new CouponService(repo, clock);
        Coupon c = new Coupon();
        c.startDate = clock.instant().minusSeconds(60);
        c.endDate = clock.instant();
        when(repo.lockByCode("EXPIRED")).thenReturn(java.util.Optional.of(c));
        assertThrows(ApiException.class, () -> s.lock("EXPIRED"));
    }
}
