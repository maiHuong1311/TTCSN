package com.learnflow.backend.catalog;

import com.learnflow.backend.catalog.dto.ProductResponse;
import java.util.*;
import org.springframework.stereotype.Component;

/** Content-based TF-IDF/cosine ranking with anonymous purchase popularity as cold start. */
@Component
public class RecommendationEngine {

    public List<ProductResponse> rank(
        List<ProductResponse> catalog,
        Map<Long, Double> history,
        String query,
        Map<Long, Long> popularity,
        Map<Long, Long> together,
        Long anchorId,
        boolean match
    ) {
        var vectors = new HashMap<Long, Map<String, Double>>();
        var df = new HashMap<String, Integer>();
        for (var p : catalog) {
            var tokens = features(p);
            vectors.put(p.productId(), tokens);
            tokens.keySet().forEach(t -> df.merge(t, 1, Integer::sum));
        }
        for (var vector : vectors.values())
            vector.replaceAll(
                (t, v) ->
                    (1 + Math.log(v)) *
                    (1 + Math.log((catalog.size() + 1.0) / (df.get(t) + 1.0)))
            );
        Map<String, Double> target = new HashMap<>();
        if (anchorId != null) target.putAll(
            vectors.getOrDefault(anchorId, Map.of())
        );
        else {
            history.forEach((id, w) ->
                vectors
                    .getOrDefault(id, Map.of())
                    .forEach((t, v) -> target.merge(t, w * v, Double::sum))
            );
            tokens(query).forEach(t -> target.merge(t, 4.0, Double::sum));
        }
        ProductResponse anchor = catalog
            .stream()
            .filter(p -> p.productId().equals(anchorId))
            .findFirst()
            .orElse(null);
        return catalog
            .stream()
            .filter(
                p ->
                    "Active".equals(p.status()) &&
                    p
                        .variants()
                        .stream()
                        .anyMatch(v -> v.stockQuantity() > 0)
            )
            .filter(p -> !p.productId().equals(anchorId))
            .filter(
                p ->
                    anchor == null ||
                    "Unisex".equals(anchor.gender()) ||
                    "Unisex".equals(p.gender()) ||
                    Objects.equals(anchor.gender(), p.gender())
            )
            .filter(p -> !match || (anchor != null && compatible(anchor, p)))
            .sorted(
                Comparator.<ProductResponse>comparingDouble(p -> {
                    double score = cosine(target, vectors.get(p.productId()));
                    if (match) score =
                        0.2 * score +
                        1.5 *
                            Math.log1p(
                                together.getOrDefault(p.productId(), 0L)
                            ) +
                        (anchor != null &&
                        Objects.equals(anchor.material(), p.material())
                            ? .15
                            : 0);
                    return (
                        score * 10 +
                        .15 *
                            Math.log1p(
                                popularity.getOrDefault(p.productId(), 0L)
                            )
                    );
                })
                    .reversed()
                    .thenComparing(
                        ProductResponse::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                    )
                    .thenComparing(
                        ProductResponse::productId,
                        Comparator.reverseOrder()
                    )
            )
            .limit(8)
            .toList();
    }

    private Map<String, Double> features(ProductResponse p) {
        Map<String, Double> v = new HashMap<>();
        add(v, p.productName(), 3);
        add(v, p.description(), 1);
        add(v, p.material(), 2);
        add(v, p.brand(), 1);
        v.put("category_" + p.categoryId(), 4.0);
        p.variants()
            .stream()
            .map(ProductResponse.VariantResponse::color)
            .distinct()
            .forEach(c -> add(v, c, 2));
        return v;
    }

    private void add(Map<String, Double> v, String text, double weight) {
        tokens(text).forEach(t -> v.merge(t, weight, Double::sum));
    }

    private List<String> tokens(String text) {
        if (text == null) return List.of();
        String normalized = java.text.Normalizer.normalize(
            text.toLowerCase(Locale.ROOT),
            java.text.Normalizer.Form.NFD
        )
            .replaceAll("\\p{M}", "")
            .replace('đ', 'd')
            .toLowerCase(Locale.ROOT);
        return Arrays.stream(normalized.split("[^a-z0-9]+"))
            .filter(t -> t.length() > 1)
            .toList();
    }

    private double cosine(Map<String, Double> a, Map<String, Double> b) {
        double dot = a
            .entrySet()
            .stream()
            .mapToDouble(e -> e.getValue() * b.getOrDefault(e.getKey(), 0.0))
            .sum();
        double normA = Math.sqrt(
            a
                .values()
                .stream()
                .mapToDouble(v -> v * v)
                .sum()
        );
        double normB = Math.sqrt(
            b
                .values()
                .stream()
                .mapToDouble(v -> v * v)
                .sum()
        );
        return normA == 0 || normB == 0 ? 0 : dot / (normA * normB);
    }

    private String group(ProductResponse p) {
        String n = String.join(" ", tokens(p.productName()));
        if (n.contains("chan vay")) return "skirt";
        if (n.contains("dam") || n.contains("vay lien")) return "dress";
        if (n.contains("ao khoac") || n.contains("cardigan")) return "layer";
        return switch (p.categoryId().intValue()) {
            case 1 -> "top";
            case 2 -> "bottom";
            case 3 -> "dress";
            default -> "accessory";
        };
    }

    private boolean compatible(ProductResponse a, ProductResponse b) {
        String x = group(a),
            y = group(b);
        if (x.equals(y)) return false;
        if (x.equals("accessory") || y.equals("accessory")) return true;
        if (x.equals("dress") || y.equals("dress")) return (
            x.equals("layer") || y.equals("layer")
        );
        return (
            !(
                Set.of("bottom", "skirt").contains(x) &&
                Set.of("bottom", "skirt").contains(y)
            ) &&
            !(
                Set.of("top", "layer").contains(x) &&
                Set.of("top", "layer").contains(y)
            )
        );
    }
}
