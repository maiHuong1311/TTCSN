package com.learnflow.backend.catalog;

import com.learnflow.backend.catalog.dto.*;
import com.learnflow.backend.common.web.PageResponse;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService service;

    public CatalogController(CatalogService s) {
        service = s;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return service.categories();
    }

    @GetMapping("/products")
    public PageResponse<ProductResponse> list(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String gender,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(q, categoryId, gender, page, size, false);
    }

    @GetMapping("/products/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/products/{id}/views")
    public void view(Authentication a, @PathVariable Long id) {
        service.track((Long) a.getPrincipal(), id);
    }

    @DeleteMapping("/interactions")
    public void clear(Authentication a) {
        service.clearTracking((Long) a.getPrincipal());
    }

    @PostMapping("/search-events")
    public void search(Authentication a, @jakarta.validation.Valid @RequestBody SearchEventRequest r) { service.trackSearch((Long)a.getPrincipal(), r.query()); }

    @GetMapping("/recommendations")
    public List<ProductResponse> recommendations(
        Authentication a,
        @RequestParam(required = false) Long productId,
        @RequestParam(defaultValue = "false") boolean match,
        @RequestParam(defaultValue = "false") boolean personalized
    ) {
        return service.suggestions((Long) a.getPrincipal(), productId, match, personalized);
    }
}
