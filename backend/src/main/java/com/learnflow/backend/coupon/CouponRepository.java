package com.learnflow.backend.coupon;

import com.learnflow.backend.coupon.domain.Coupon;

public interface CouponRepository
    extends org.springframework.data.jpa.repository.JpaRepository<Coupon, Long>
{
    @org.springframework.data.jpa.repository.Query("""
        select c from Coupon c
        where c.startDate <= :now and c.endDate > :now
          and (select count(o) from ShopOrder o where o.couponId = c.id) < c.usageLimit
          and (:userId is null or not exists (
              select used.id from ShopOrder used where used.couponId = c.id and used.userId = :userId
          ))
        order by c.endDate asc, c.id asc
        """)
    java.util.List<Coupon> findAvailable(
        @org.springframework.data.repository.query.Param("now") java.time.Instant now,
        @org.springframework.data.repository.query.Param("userId") Long userId
    );

    java.util.Optional<Coupon> findByCouponCodeIgnoreCase(String code);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query(
        "select c from Coupon c where lower(c.couponCode)=lower(:code)"
    )
    java.util.Optional<Coupon> lockByCode(
        @org.springframework.data.repository.query.Param("code") String code
    );
}
