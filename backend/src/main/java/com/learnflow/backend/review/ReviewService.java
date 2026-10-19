package com.learnflow.backend.review;

import com.learnflow.backend.auth.AuthService;
import com.learnflow.backend.common.web.ApiException;
import com.learnflow.backend.order.OrderService;
import com.learnflow.backend.review.domain.Review;
import com.learnflow.backend.review.dto.*;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository repository;
    private final OrderService orders;
    private final AuthService users;
    private final Clock clock;

    public ReviewService(ReviewRepository r, OrderService o, AuthService u, Clock c) {
        repository = r;
        orders = o;
        users = u;
        clock = c;
    }

    public List<ReviewResponse> list(Long p) {
        return repository.findByProductIdOrderByCreatedAtDesc(p).stream().map(this::dto).toList();
    }

    public ReviewResponse create(Long u, Long p, ReviewRequest r) {
        if (!orders.purchased(u, p)) throw new ApiException(
            HttpStatus.FORBIDDEN,
            "Bạn chỉ có thể đánh giá sản phẩm đã mua sau khi đơn hàng được giao thành công."
        );
        if (repository.existsByUserIdAndProductId(u, p)) throw new ApiException(
            HttpStatus.CONFLICT,
            "Bạn đã đánh giá sản phẩm này."
        );
        Review x = new Review();
        x.userId = u;
        x.productId = p;
        x.rating = r.rating();
        x.comment = r.comment();
        x.imageUrl = r.imageUrl();
        x.createdAt = clock.instant();
        return dto(repository.save(x));
    }

    @Transactional(readOnly = true)
    public ReviewEligibilityResponse eligibility(Long userId, Long productId) {
        boolean delivered = orders.purchased(userId, productId);
        boolean reviewed = repository.existsByUserIdAndProductId(userId, productId);
        return new ReviewEligibilityResponse(delivered && !reviewed, delivered, reviewed);
    }

    private ReviewResponse dto(Review x) {
        return new ReviewResponse(
            x.id,
            x.userId,
            users.displayName(x.userId),
            x.productId,
            x.rating,
            x.comment,
            x.imageUrl,
            x.createdAt
        );
    }
}
