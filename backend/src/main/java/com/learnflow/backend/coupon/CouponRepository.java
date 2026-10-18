package com.learnflow.backend.coupon;

import com.learnflow.backend.coupon.domain.Coupon;

public interface CouponRepository
    extends org.springframework.data.jpa.repository.JpaRepository<Coupon, Long>
{
    java.util.Optional<Coupon> findByCouponCodeIgnoreCase(String code);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query(
        "select c from Coupon c where lower(c.couponCode)=lower(:code)"
    )
    java.util.Optional<Coupon> lockByCode(
        @org.springframework.data.repository.query.Param("code") String code
    );
}
