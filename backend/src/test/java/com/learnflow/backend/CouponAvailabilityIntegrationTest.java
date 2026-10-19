package com.learnflow.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.learnflow.backend.coupon.CouponRepository;
import com.learnflow.backend.coupon.domain.Coupon;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CouponAvailabilityIntegrationTest extends AbstractIntegrationTest {
    @Autowired CouponRepository coupons;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockBean Clock clock;

    @Test
    void publicListExcludesFutureExpiredAndExhaustedCouponsAndKeepsAdminPrivate() throws Exception {
        Instant now = Instant.parse("2026-10-19T00:00:00Z");
        when(clock.instant()).thenReturn(now);
        jdbc.update("UPDATE coupon SET usage_limit=0");
        save("LIVE", now, now.plusSeconds(60), 2);
        save("FUTURE", now.plusSeconds(1), now.plusSeconds(60), 2);
        save("EXPIRED", now.minusSeconds(60), now, 2);
        save("ZERO", now.minusSeconds(60), now.plusSeconds(60), 0);
        Coupon spent = save("SPENT", now.minusSeconds(60), now.plusSeconds(60), 1);
        jdbc.update("INSERT INTO app_user(email,password_hash,full_name,role,created_at,username,status,auth_version) VALUES ('coupon@test.local','unused','Test','Customer',UTC_TIMESTAMP(),'coupon_test','Active',0)");
        Long userId = jdbc.queryForObject("SELECT id FROM app_user WHERE email='coupon@test.local'", Long.class);
        jdbc.update("INSERT INTO shop_order(user_id,receiver_name,receiver_phone,shipping_address,total_amount,shipping_fee,discount_amount,order_status,created_at,coupon_id) VALUES (?,'Test','0901234567','Test',100,0,0,'Pending',UTC_TIMESTAMP(),?)", userId, spent.id);
        mvc.perform(get("/api/coupons"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].couponCode").value("LIVE"));
        mvc.perform(get("/api/admin/coupons")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/coupons").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        jdbc.update("UPDATE coupon SET usage_limit=0");
        mvc.perform(get("/api/coupons"))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    private Coupon save(String code, Instant start, Instant end, int limit) {
        Coupon c = new Coupon();
        c.couponCode = code;
        c.discountType = "Percentage";
        c.discountValue = BigDecimal.TEN;
        c.minOrderValue = new BigDecimal("300000");
        c.usageLimit = limit;
        c.startDate = start;
        c.endDate = end;
        return coupons.saveAndFlush(c);
    }
}
