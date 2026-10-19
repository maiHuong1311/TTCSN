package com.learnflow.backend.review;

import com.learnflow.backend.review.domain.Review;

public interface ReviewRepository
    extends org.springframework.data.jpa.repository.JpaRepository<Review, Long>
{
    java.util.List<Review> findByProductIdOrderByCreatedAtDesc(Long id);
    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
