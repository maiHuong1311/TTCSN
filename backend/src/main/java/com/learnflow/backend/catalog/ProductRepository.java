package com.learnflow.backend.catalog;

import com.learnflow.backend.catalog.domain.Product;

public interface ProductRepository
    extends
        org.springframework.data.jpa.repository.JpaRepository<Product, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Product>
{
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Product p where p.id=:id")
    java.util.Optional<Product> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    org.springframework.data.domain.Page<Product> findByStatusAndProductNameContainingIgnoreCase(
        String status,
        String q,
        org.springframework.data.domain.Pageable page
    );
}
