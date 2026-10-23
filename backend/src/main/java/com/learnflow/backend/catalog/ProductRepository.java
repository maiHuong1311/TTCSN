package com.learnflow.backend.catalog;

import com.learnflow.backend.catalog.domain.Product;

public interface ProductRepository
    extends
        org.springframework.data.jpa.repository.JpaRepository<Product, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Product>
{
    @org.springframework.data.jpa.repository.Query(value="SELECT v.product_id,SUM(i.quantity) FROM order_item i JOIN product_variant v ON v.variant_id=i.variant_id JOIN shop_order o ON o.order_id=i.order_id WHERE o.order_status='Delivered' GROUP BY v.product_id",nativeQuery=true)
    java.util.List<Object[]> purchasePopularity();

    @org.springframework.data.jpa.repository.Query(value="SELECT v2.product_id,COUNT(DISTINCT a.order_id) FROM order_item a JOIN product_variant v1 ON v1.variant_id=a.variant_id JOIN order_item b ON b.order_id=a.order_id JOIN product_variant v2 ON v2.variant_id=b.variant_id JOIN shop_order o ON o.order_id=a.order_id WHERE o.order_status='Delivered' AND v1.product_id=:id AND v2.product_id<>:id GROUP BY v2.product_id",nativeQuery=true)
    java.util.List<Object[]> frequentlyBoughtTogether(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Product p where p.id=:id")
    java.util.Optional<Product> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    org.springframework.data.domain.Page<Product> findByStatusAndProductNameContainingIgnoreCase(
        String status,
        String q,
        org.springframework.data.domain.Pageable page
    );
}
