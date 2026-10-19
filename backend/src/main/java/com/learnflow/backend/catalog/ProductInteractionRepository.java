package com.learnflow.backend.catalog;

import com.learnflow.backend.catalog.domain.ProductInteraction;

public interface ProductInteractionRepository
    extends org.springframework.data.jpa.repository.JpaRepository<ProductInteraction, Long>
{
    java.util.List<ProductInteraction> findTop20ByUserIdOrderByCreatedAtDesc(Long id);
    java.util.List<ProductInteraction> findByUserId(Long id);

    @org.springframework.data.jpa.repository.Query(
        "select x.productId,count(x) from ProductInteraction x group by x.productId order by count(x) desc"
    )
    java.util.List<Object[]> summarize();
}
