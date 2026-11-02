package com.learnflow.backend.catalog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learnflow.backend.catalog.domain.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogServiceBatchTest {
    @Test
    void loadsVariantsAndImagesInBatchesAndExcludesInactiveOrSoldOutProducts() {
        var products = mock(ProductRepository.class);
        var variants = mock(ProductVariantRepository.class);
        var images = mock(ProductImageRepository.class);
        var service = new CatalogService(null, products, variants, images, null,
            java.time.Clock.systemUTC(), null, null);
        var active = new Product();
        active.id = 1L;
        active.status = "Active";
        var inactive = new Product();
        inactive.id = 2L;
        inactive.status = "Inactive";
        var soldOut = new Product();
        soldOut.id = 3L;
        soldOut.status = "Active";
        var stock = new ProductVariant();
        stock.id = 11L;
        stock.productId = 1L;
        stock.stockQuantity = 2;
        var empty = new ProductVariant();
        empty.id = 12L;
        empty.productId = 3L;
        empty.stockQuantity = 0;
        var image = new ProductImage();
        image.id = 21L;
        image.productId = 1L;
        image.isPrimary = true;
        when(products.findAll()).thenReturn(List.of(active, inactive, soldOut));
        when(variants.findByProductIdInOrderById(List.of(1L, 3L))).thenReturn(List.of(stock, empty));
        when(images.findByProductIdInOrderById(List.of(1L, 3L))).thenReturn(List.of(image));

        var result = service.availableCatalog();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).productId());
        assertEquals(11L, result.get(0).variants().get(0).variantId());
        assertEquals(21L, result.get(0).images().get(0).imageId());
        verify(products).findAll();
        verify(variants).findByProductIdInOrderById(List.of(1L, 3L));
        verify(images).findByProductIdInOrderById(List.of(1L, 3L));
        verifyNoMoreInteractions(products, variants, images);
    }
}
