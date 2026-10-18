package com.learnflow.backend.catalog;

import com.learnflow.backend.catalog.domain.ProductVariant;

public interface ProductVariantRepository
    extends org.springframework.data.jpa.repository.JpaRepository<ProductVariant, Long>
{
    java.util.List<ProductVariant> findByProductIdOrderById(Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from ProductVariant v where v.id=:id")
    java.util.Optional<ProductVariant> lockById(
        @org.springframework.data.repository.query.Param("id") Long id
    );
}
