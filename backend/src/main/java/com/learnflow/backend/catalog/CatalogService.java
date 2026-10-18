package com.learnflow.backend.catalog;

import com.learnflow.backend.catalog.domain.*;
import com.learnflow.backend.catalog.dto.*;
import com.learnflow.backend.common.web.*;
import java.time.Clock;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogService {

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ProductImageRepository images;
    private final ProductInteractionRepository interactions;
    private final Clock clock;

    public CatalogService(
        CategoryRepository c,
        ProductRepository p,
        ProductVariantRepository v,
        ProductImageRepository i,
        ProductInteractionRepository x,
        Clock clock
    ) {
        categories = c;
        products = p;
        variants = v;
        images = i;
        interactions = x;
        this.clock = clock;
    }

    public List<CategoryResponse> categories() {
        return categories
            .findAll()
            .stream()
            .map(c -> new CategoryResponse(c.id, c.categoryName, c.parentId, c.description))
            .toList();
    }

    public CategoryResponse saveCategory(Long id, CategoryRequest r) {
        Category c = id == null ? new Category() : categories.findById(id).orElseThrow(() -> missing());
        if (r.parentId() != null) {
            Long ancestor = r.parentId();
            Set<Long> seen = new HashSet<>();
            while (ancestor != null) {
                if (ancestor.equals(id) || !seen.add(ancestor)) throw bad("Category cycle");
                ancestor = categories.findById(ancestor).orElseThrow(() -> missing()).parentId;
            }
        }
        c.categoryName = r.categoryName();
        c.description = r.description();
        c.parentId = r.parentId();
        categories.save(c);
        return new CategoryResponse(c.id, c.categoryName, c.parentId, c.description);
    }

    public void deleteCategory(Long id) {
        categories.deleteById(id);
        categories.flush();
    }

    public PageResponse<ProductResponse> list(
        String q,
        Long categoryId,
        String gender,
        int page,
        int size,
        boolean admin
    ) {
        if (page < 0 || size < 1 || size > 100) throw bad("Invalid pagination");
        var spec = (org.springframework.data.jpa.domain.Specification<Product>) (root, query, cb) -> {
            var rules = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!admin) rules.add(cb.equal(root.get("status"), "Active"));
            if (q != null && !q.isBlank()) rules.add(
                cb.like(cb.lower(root.get("productName")), "%" + q.toLowerCase() + "%")
            );
            if (categoryId != null) rules.add(cb.equal(root.get("categoryId"), categoryId));
            if (gender != null && !gender.isBlank()) rules.add(cb.equal(root.get("gender"), gender));
            return cb.and(rules.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return PageResponse.from(
            products
                .findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .map(this::response)
        );
    }

    public ProductResponse get(Long id) {
        Product p = product(id);
        if (!"Active".equals(p.status)) throw missing();
        return response(p);
    }

    public ProductResponse snapshot(Long id) {
        return response(product(id));
    }

    public Product product(Long id) {
        return products.findById(id).orElseThrow(() -> missing());
    }

    public ProductVariant variant(Long id) {
        return variants.findById(id).orElseThrow(() -> missing());
    }

    public ProductVariant lockVariant(Long id) {
        ProductVariant variant = variants.lockById(id).orElseThrow(() -> missing());
        // A locking query alone can reuse a stale entity already in the persistence context.
        entityManager.refresh(variant, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return variant;
    }

    public Product lockProduct(Long id) {
        Product product = products.lockById(id).orElseThrow(() -> missing());
        entityManager.refresh(product, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return product;
    }

    public ProductResponse save(Long id, ProductRequest r) {
        if (!categories.existsById(r.categoryId())) throw bad("Category does not exist");
        if (r.salePrice() != null && r.salePrice().compareTo(r.price()) > 0) throw bad(
            "Sale price exceeds price"
        );
        Product p = id == null ? new Product() : lockProduct(id);
        if (id == null) p.createdAt = clock.instant();
        p.updatedAt = clock.instant();
        p.categoryId = r.categoryId();
        p.productName = r.productName();
        p.description = r.description();
        p.brand = r.brand();
        p.material = r.material();
        p.gender = r.gender();
        p.price = r.price();
        p.salePrice = r.salePrice();
        p.status = r.status();
        products.save(p);
        var previous = variants.findByProductIdOrderById(p.id);
        Set<Long> kept = new HashSet<>();
        for (var item : r.variants()) {
            ProductVariant v =
                item.variantId() == null ? new ProductVariant() : lockVariant(item.variantId());
            if (v.id != null && !p.id.equals(v.productId)) throw bad("Variant belongs to another product");
            if (v.id != null && !kept.add(v.id)) throw bad("Duplicate variant");
            v.productId = p.id;
            v.size = item.size() == null || item.size().isBlank() ? "Freesize" : item.size().trim();
            v.color = item.color();
            v.stockQuantity = item.stockQuantity();
            v.sku = item.sku();
            v.imageUrl = item.imageUrl();
            variants.save(v);
        }
        if (previous.stream().anyMatch(v -> !kept.contains(v.id))) throw bad(
            "Keep existing variants; set stock to zero to retire them"
        );
        images.deleteAll(images.findByProductId(p.id));
        images.flush();
        if (r.images() != null) for (int n = 0; n < r.images().size(); n++) {
            ProductImage i = new ProductImage();
            i.productId = p.id;
            i.imageUrl = r.images().get(n);
            i.isPrimary = n == 0;
            images.save(i);
        }
        return response(p);
    }

    public void deactivate(Long id) {
        Product p = lockProduct(id);
        p.status = "Inactive";
        p.updatedAt = clock.instant();
    }

    public void takeStock(Long variantId, int quantity) {
        ProductVariant v = lockVariant(variantId);
        if (
            quantity < 1 || v.stockQuantity < quantity || !"Active".equals(product(v.productId).status)
        ) throw bad("Product unavailable or insufficient stock");
        v.stockQuantity -= quantity;
    }

    public void restoreStock(Long variantId, int quantity) {
        lockVariant(variantId).stockQuantity += quantity;
    }

    public void track(Long userId, Long productId) {
        get(productId);
        ProductInteraction x = new ProductInteraction();
        x.userId = userId;
        x.productId = productId;
        x.eventType = "View";
        x.createdAt = clock.instant();
        interactions.save(x);
    }

    public void clearTracking(Long userId) {
        interactions.deleteAll(interactions.findByUserId(userId));
    }

    public List<ProductResponse> suggestions(Long userId, Long productId, boolean match) {
        List<Product> all = products
            .findAll()
            .stream()
            .filter(p -> "Active".equals(p.status))
            .toList();
        Long preferred =
            productId == null
                ? interactions
                      .findTop20ByUserIdOrderByCreatedAtDesc(userId)
                      .stream()
                      .findFirst()
                      .map(x -> product(x.productId).categoryId)
                      .orElse(null)
                : product(productId).categoryId;
        return all
            .stream()
            .filter(p -> !p.id.equals(productId))
            .sorted(
                Comparator.<Product>comparingInt(p ->
                    Objects.equals(p.categoryId, preferred) ? (match ? 1 : 0) : match ? 0 : 1
                ).thenComparing(p -> p.createdAt, Comparator.reverseOrder())
            )
            .limit(8)
            .map(this::response)
            .toList();
    }

    public List<com.learnflow.backend.catalog.dto.BehaviorResponse> behavior() {
        return interactions
            .summarize()
            .stream()
            .map(x ->
                new com.learnflow.backend.catalog.dto.BehaviorResponse(
                    (Long) x[0],
                    product((Long) x[0]).productName,
                    (Long) x[1]
                )
            )
            .toList();
    }

    private ProductResponse response(Product p) {
        return new ProductResponse(
            p.id,
            p.categoryId,
            p.productName,
            p.description,
            p.brand,
            p.material,
            p.gender,
            p.price,
            p.salePrice,
            p.status,
            p.createdAt,
            p.updatedAt,
            variants
                .findByProductIdOrderById(p.id)
                .stream()
                .map(v ->
                    new ProductResponse.VariantResponse(
                        v.id,
                        v.size,
                        v.color,
                        v.stockQuantity,
                        v.sku,
                        v.imageUrl
                    )
                )
                .toList(),
            images
                .findByProductId(p.id)
                .stream()
                .map(i -> new ProductResponse.ImageResponse(i.id, i.imageUrl, i.isPrimary))
                .toList()
        );
    }

    private ApiException missing() {
        return new ApiException(HttpStatus.NOT_FOUND, "Catalog item not found");
    }

    private ApiException bad(String m) {
        return new ApiException(HttpStatus.BAD_REQUEST, m);
    }
}
