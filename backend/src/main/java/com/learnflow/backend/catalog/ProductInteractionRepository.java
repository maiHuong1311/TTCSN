package com.learnflow.backend.catalog;

import com.learnflow.backend.catalog.domain.ProductInteraction;

public interface ProductInteractionRepository
    extends org.springframework.data.jpa.repository.JpaRepository<ProductInteraction, Long>
{
    java.util.List<ProductInteraction> findTop20ByUserIdOrderByCreatedAtDesc(Long id);
    java.util.List<ProductInteraction> findByUserId(Long id);
    java.util.List<ProductInteraction> findTop100ByUserIdOrderByCreatedAtDesc(Long id);
    void deleteByUserIdAndCreatedAtBefore(Long userId, java.time.Instant cutoff);
    void deleteByCreatedAtBefore(java.time.Instant cutoff);

    @org.springframework.data.jpa.repository.Query(
        "select x.productId,count(x) from ProductInteraction x where x.productId is not null and x.eventType='View' group by x.productId order by count(x) desc"
    )
    java.util.List<Object[]> summarize();
}
