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
    private final RecommendationEngine recommender;
    private final com.learnflow.backend.auth.AuthService users;

    public CatalogService(
        CategoryRepository c,
        ProductRepository p,
        ProductVariantRepository v,
        ProductImageRepository i,
        ProductInteractionRepository x,
        Clock clock,
        RecommendationEngine recommender,
        com.learnflow.backend.auth.AuthService users
    ) {
        categories = c;
        products = p;
        variants = v;
        images = i;
        interactions = x;
        this.clock = clock;
        this.recommender = recommender;
        this.users = users;
    }

    public List<CategoryResponse> categories() {
        return categories
            .findAll()
            .stream()
            .map(c ->
                new CategoryResponse(
                    c.id,
                    c.categoryName,
                    c.parentId,
                    c.description
                )
            )
            .toList();
    }

    public CategoryResponse saveCategory(Long id, CategoryRequest r) {
        Category c =
            id == null
                ? new Category()
                : categories.findById(id).orElseThrow(() -> missing());
        if (r.parentId() != null) {
            Long ancestor = r.parentId();
            Set<Long> seen = new HashSet<>();
            while (ancestor != null) {
                if (ancestor.equals(id) || !seen.add(ancestor)) throw bad(
                    "Category cycle"
                );
                ancestor = categories
                    .findById(ancestor)
                    .orElseThrow(() -> missing())
                    .parentId;
            }
        }
        c.categoryName = r.categoryName();
        c.description = r.description();
        c.parentId = r.parentId();
        categories.save(c);
        return new CategoryResponse(
            c.id,
            c.categoryName,
            c.parentId,
            c.description
        );
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
        var spec =
            (org.springframework.data.jpa.domain.Specification<Product>) (
                root,
                query,
                cb
            ) -> {
                var rules =
                    new ArrayList<jakarta.persistence.criteria.Predicate>();
                if (!admin) rules.add(cb.equal(root.get("status"), "Active"));
                if (q != null && !q.isBlank()) rules.add(
                    cb.like(
                        cb.lower(root.get("productName")),
                        "%" + q.toLowerCase() + "%"
                    )
                );
                if (categoryId != null) rules.add(
                    cb.equal(root.get("categoryId"), categoryId)
                );
                if (gender != null && !gender.isBlank()) rules.add(
                    cb.equal(root.get("gender"), gender)
                );
                return cb.and(
                    rules.toArray(jakarta.persistence.criteria.Predicate[]::new)
                );
            };
        return PageResponse.from(
            products
                .findAll(
                    spec,
                    PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "createdAt", "id")
                    )
                )
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
        ProductVariant variant = variants
            .lockById(id)
            .orElseThrow(() -> missing());
        // A locking query alone can reuse a stale entity already in the persistence context.
        entityManager.refresh(
            variant,
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE
        );
        return variant;
    }

    public Product lockProduct(Long id) {
        Product product = products.lockById(id).orElseThrow(() -> missing());
        entityManager.refresh(
            product,
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE
        );
        return product;
    }

    public ProductResponse save(Long id, ProductRequest r) {
        if (!categories.existsById(r.categoryId())) throw bad(
            "Category does not exist"
        );
        if (
            r.salePrice() != null && r.salePrice().compareTo(r.price()) > 0
        ) throw bad("Sale price exceeds price");
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
        p.sizeChartCode = r.sizeChartCode();
        products.save(p);
        var previous = variants.findByProductIdOrderById(p.id);
        Set<Long> kept = new HashSet<>();
        for (var item : r.variants()) {
            ProductVariant v =
                item.variantId() == null
                    ? new ProductVariant()
                    : lockVariant(item.variantId());
            if (v.id != null && !p.id.equals(v.productId)) throw bad(
                "Variant belongs to another product"
            );
            if (v.id != null && !kept.add(v.id)) throw bad("Duplicate variant");
            v.productId = p.id;
            v.size =
                item.size() == null || item.size().isBlank()
                    ? "Freesize"
                    : item.size().trim();
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
            quantity < 1 ||
            v.stockQuantity < quantity ||
            !"Active".equals(product(v.productId).status)
        ) throw bad("Product unavailable or insufficient stock");
        v.stockQuantity -= quantity;
    }

    public void restoreStock(Long variantId, int quantity) {
        lockVariant(variantId).stockQuantity += quantity;
    }

    public void track(Long userId, Long productId) {
        trackEvent(userId, productId, "View");
    }

    public void trackEvent(Long userId, Long productId, String type) {
        get(productId);
        if (!Set.of("View", "Cart", "Purchase").contains(type)) throw bad(
            "Invalid event type"
        );
        recordInteraction(userId, productId, type, null);
    }

    public void trackSearch(Long userId, String query) {
        String cleaned = query
            .trim()
            .replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+", "")
            .replaceAll("[+]?\\d[\\d .-]{8,}\\d", "")
            .trim();
        if (cleaned.length() < 2) return;
        recordInteraction(userId, null, "Search", cleaned);
    }

    private void recordInteraction(
        Long userId,
        Long productId,
        String type,
        String query
    ) {
        var cutoff = clock.instant().minusSeconds(30);
        if (
            interactions
                .findTop100ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .anyMatch(
                    x ->
                        x.createdAt.isAfter(cutoff) &&
                        Objects.equals(x.productId, productId) &&
                        type.equals(x.eventType) &&
                        Objects.equals(x.queryText, query)
                )
        ) return;
        interactions.deleteByUserIdAndCreatedAtBefore(
            userId,
            clock.instant().minusSeconds(90L * 86400)
        );
        ProductInteraction x = new ProductInteraction();
        x.userId = userId;
        x.productId = productId;
        x.eventType = type;
        x.queryText = query;
        x.createdAt = clock.instant();
        interactions.save(x);
    }

    public void clearTracking(Long userId) {
        interactions.deleteAll(interactions.findByUserId(userId));
    }

    @org.springframework.scheduling.annotation.Scheduled(
        fixedDelay = 3600000,
        initialDelay = 60000
    )
    public void purgeOldInteractions() {
        interactions.deleteByCreatedAtBefore(
            clock.instant().minusSeconds(90L * 86400)
        );
    }

    public List<ProductResponse> availableCatalog() {
        return products
            .findAll()
            .stream()
            .filter(p -> "Active".equals(p.status))
            .map(this::response)
            .filter(p ->
                p
                    .variants()
                    .stream()
                    .anyMatch(v -> v.stockQuantity() > 0)
            )
            .toList();
    }

    public List<ProductResponse> suggestions(
        Long userId,
        Long productId,
        boolean match
    ) {
        return suggestions(userId, productId, match, false);
    }

    public List<ProductResponse> suggestions(
        Long userId,
        Long productId,
        boolean match,
        boolean personalized
    ) {
        var all = availableCatalog();
        Map<Long, Double> weights = new HashMap<>();
        StringBuilder searches = new StringBuilder();
        if (
            personalized && productId == null
        ) for (var x : interactions.findTop100ByUserIdOrderByCreatedAtDesc(
            userId
        )) {
            long age = Math.max(
                0,
                java.time.Duration.between(
                    x.createdAt,
                    clock.instant()
                ).toDays()
            );
            if (age > 90) continue;
            if ("Search".equals(x.eventType) && x.queryText != null) searches
                .append(' ')
                .append(x.queryText);
            if (x.productId != null) weights.merge(
                x.productId,
                ("Purchase".equals(x.eventType)
                    ? 5.0
                    : "Cart".equals(x.eventType)
                      ? 3.0
                      : 1.0) /
                    (1 + age / 7.0),
                Double::sum
            );
        }
        if (personalized && productId == null) {
            String style = users.requireUser(userId).getPreferredStyle();
            if (style != null) searches.append(' ').append(style);
        }
        var popularity = counts(products.purchasePopularity());
        var together =
            productId != null && match
                ? counts(products.frequentlyBoughtTogether(productId))
                : Map.<Long, Long>of();
        return recommender.rank(
            all,
            weights,
            searches.toString(),
            popularity,
            together,
            productId,
            match
        );
    }

    public List<ProductResponse> contextSuggestions(String query) {
        var maximum = maximumPrice(query);
        var candidates = availableCatalog()
            .stream()
            .filter(
                p ->
                    maximum == null ||
                    (p.salePrice() == null
                        ? p.price()
                        : p.salePrice()
                    ).compareTo(maximum) <= 0
            )
            .toList();
        return recommender.rank(
            candidates,
            Map.of(),
            query,
            counts(products.purchasePopularity()),
            Map.of(),
            null,
            false
        );
    }

    public java.math.BigDecimal maximumPrice(String query) {
        String normalized = java.text.Normalizer.normalize(
            query.toLowerCase(Locale.ROOT),
            java.text.Normalizer.Form.NFD
        )
            .replaceAll("\\p{M}", "")
            .replace('đ', 'd');
        var matcher = java.util.regex.Pattern.compile(
            "(?:duoi|toi da|khong qua|ngan sach|tam)\\s*([0-9.,]+)\\s*(trieu|tr|nghin|ngan|k|dong|d|₫)?"
        ).matcher(normalized);
        if (!matcher.find()) return null;
        try {
            String raw = matcher.group(1),
                unit = Objects.toString(matcher.group(2), "");
            java.math.BigDecimal value;
            if (
                Set.of("trieu", "tr", "nghin", "ngan", "k").contains(unit)
            ) value = new java.math.BigDecimal(raw.replace(',', '.')).multiply(
                java.math.BigDecimal.valueOf(
                    Set.of("trieu", "tr").contains(unit) ? 1000000 : 1000
                )
            );
            else value = new java.math.BigDecimal(
                raw.replace(".", "").replace(",", "")
            );
            return value.signum() >= 0 &&
                value.compareTo(new java.math.BigDecimal("9999999999")) <= 0
                ? value
                : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<Long, Long> counts(List<Object[]> rows) {
        Map<Long, Long> result = new HashMap<>();
        for (var row : rows)
            result.put(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue()
            );
        return result;
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
                .map(i ->
                    new ProductResponse.ImageResponse(
                        i.id,
                        i.imageUrl,
                        i.isPrimary
                    )
                )
                .toList(),
            p.sizeChartCode
        );
    }

    private ApiException missing() {
        return new ApiException(HttpStatus.NOT_FOUND, "Catalog item not found");
    }

    private ApiException bad(String m) {
        return new ApiException(HttpStatus.BAD_REQUEST, m);
    }
}
