package com.learnflow.backend.catalog;

import com.learnflow.backend.catalog.domain.ProductImage;

public interface ProductImageRepository
    extends org.springframework.data.jpa.repository.JpaRepository<ProductImage, Long>
{
    java.util.List<ProductImage> findByProductId(Long id);
}
