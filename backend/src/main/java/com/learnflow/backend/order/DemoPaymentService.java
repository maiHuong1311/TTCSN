package com.learnflow.backend.order;

import com.learnflow.backend.common.web.ApiException;
import com.learnflow.backend.order.domain.Payment;
import com.learnflow.backend.order.domain.ShopOrder;
import com.learnflow.backend.order.dto.DemoPaymentResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Clock;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class DemoPaymentService {

    private final ShopOrderRepository orders;
    private final PaymentRepository payments;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public DemoPaymentService(ShopOrderRepository orders, PaymentRepository payments, Clock clock) {
        this.orders = orders;
        this.payments = payments;
        this.clock = clock;
    }

    public DemoPaymentResponse create(Long userId, Long orderId) {
        var order = orders.lockById(orderId).orElseThrow(this::missing);
        if (!order.userId.equals(userId)) throw missing();
        var payment = payments.findByOrderId(orderId).orElseThrow(this::missing);
        requireDemo(payment);
        requireOpen(order);
        if (!Set.of("Pending", "Failed").contains(payment.paymentStatus)) throw bad(
            "Đơn hàng đã được thanh toán."
        );
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        payment.demoTokenHash = hash(token);
        payment.demoExpiresAt = clock.instant().plusSeconds(900);
        payment.paymentStatus = "Pending";
        payment.transactionId = null;
        return response(order, payment, token);
    }

    public DemoPaymentResponse view(String token) {
        return access(token, null);
    }

    public DemoPaymentResponse complete(String token, String outcome) {
        if (outcome == null || !Set.of("Paid", "Failed").contains(outcome)) throw bad(
            "Vui lòng chọn kết quả thanh toán."
        );
        return access(token, outcome);
    }

    private DemoPaymentResponse access(String token, String outcome) {
        var found = payments.findByDemoTokenHash(hash(token)).orElseThrow(this::missing);
        var order = orders.lockById(found.orderId).orElseThrow(this::missing);
        // Re-read under the order lock, also used by cancellation/admin updates.
        var payment = payments.findByOrderId(order.id).orElseThrow(this::missing);
        entityManager.refresh(payment);
        if (!Objects.equals(hash(token), payment.demoTokenHash)) throw missing();
        requireDemo(payment);
        if (!clock.instant().isBefore(payment.demoExpiresAt)) throw bad(
            "Mã QR đã hết hạn. Hãy tạo mã mới từ đơn hàng."
        );
        if (outcome != null) {
            requireOpen(order);
            if ("Pending".equals(payment.paymentStatus)) {
                payment.paymentStatus = outcome;
                payment.transactionId = "DEMO-" + UUID.randomUUID();
                payment.paidAt = "Paid".equals(outcome) ? clock.instant() : null;
            } else if (!outcome.equals(payment.paymentStatus)) throw bad(
                "Phiên thanh toán đã kết thúc với kết quả khác."
            );
        }
        return response(order, payment, null);
    }

    private void requireDemo(Payment p) {
        if (!"DemoQR".equals(p.paymentMethod)) throw bad("Phương thức này không hỗ trợ thanh toán giả lập.");
    }

    private void requireOpen(ShopOrder o) {
        if (!Set.of("Pending", "Confirmed").contains(o.orderStatus)) throw bad(
            "Đơn hàng không còn cho phép thanh toán."
        );
    }

    private DemoPaymentResponse response(ShopOrder o, Payment p, String token) {
        return new DemoPaymentResponse(
            o.id,
            p.amount,
            p.paymentStatus,
            o.orderStatus,
            p.demoExpiresAt,
            token
        );
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private ApiException missing() {
        return new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên thanh toán.");
    }

    private ApiException bad(String text) {
        return new ApiException(HttpStatus.BAD_REQUEST, text);
    }
}
