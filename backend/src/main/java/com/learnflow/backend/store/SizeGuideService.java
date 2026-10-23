package com.learnflow.backend.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class SizeGuideService {

    public record Range(BigDecimal min, BigDecimal max) {}

    public record Row(String size, Map<String, Range> ranges) {}

    public record Chart(
        int id,
        String code,
        String name,
        String appliesTo,
        List<String> measurements,
        List<Row> rows
    ) {}

    public record Advice(
        String chartCode,
        String chartName,
        String size,
        String explanation
    ) {}

    private final List<Chart> charts;

    public SizeGuideService(ObjectMapper mapper) throws java.io.IOException {
        try (
            var stream = new ClassPathResource(
                "fashion/size-charts.json"
            ).getInputStream()
        ) {
            charts = List.copyOf(
                mapper.readValue(stream, new TypeReference<List<Chart>>() {})
            );
        }
    }

    public List<Chart> charts() {
        return charts;
    }

    public Optional<Chart> find(String code) {
        return charts
            .stream()
            .filter(c -> c.code().equals(code))
            .findFirst();
    }

    public Advice advise(
        String code,
        Map<String, BigDecimal> measures,
        Set<String> stockSizes
    ) {
        Chart chart = find(code).orElse(null);
        if (chart == null) return new Advice(
            code,
            null,
            null,
            "Sản phẩm chưa có bảng size phù hợp. Vui lòng liên hệ cửa hàng để được tư vấn."
        );
        List<String> missing = chart
            .measurements()
            .stream()
            .filter(m -> measures.get(m) == null)
            .map(SizeGuideService::label)
            .toList();
        if (!missing.isEmpty()) return new Advice(
            code,
            chart.name(),
            null,
            "Vui lòng bổ sung " +
                String.join(", ", missing) +
                " để đối chiếu bảng size. Chiều cao và cân nặng không thay thế các số đo này."
        );
        int chosen = 0;
        for (String m : chart.measurements()) {
            BigDecimal value = measures.get(m);
            int index = -1;
            for (int n = 0; n < chart.rows().size(); n++) {
                Range range = chart.rows().get(n).ranges().get(m);
                // At shared boundaries choose the larger size. The final upper bound is inclusive.
                if (
                    value.compareTo(range.min()) >= 0 &&
                    (value.compareTo(range.max()) < 0 ||
                        (n == chart.rows().size() - 1 &&
                            value.compareTo(range.max()) == 0))
                ) index = n;
            }
            if (index < 0) return new Advice(
                code,
                chart.name(),
                null,
                "Số đo " +
                    label(m) +
                    " nằm ngoài bảng size đã công bố. Vui lòng liên hệ cửa hàng; hệ thống không tự ước lượng size ngoài bảng."
            );
            chosen = Math.max(chosen, index);
        }
        Row row = chart.rows().get(chosen);
        if (
            stockSizes != null &&
            stockSizes.stream().noneMatch(s -> row.size().equalsIgnoreCase(s))
        ) return new Advice(
            code,
            chart.name(),
            null,
            "Theo số đo, size tham khảo là " +
                row.size() +
                ", nhưng size này hiện không còn hàng ở sản phẩm đã chọn. Không tự thay bằng size khác."
        );
        boolean mixed = chart
            .measurements()
            .stream()
            .anyMatch(
                m -> measures.get(m).compareTo(row.ranges().get(m).min()) < 0
            );
        return new Advice(
            code,
            chart.name(),
            row.size(),
            "Đối chiếu bảng " +
                chart.name() +
                ": gợi ý size " +
                row.size() +
                ". " +
                (mixed
                    ? "Các số đo thuộc nhiều cỡ; chọn theo số đo lớn hơn nên một số vị trí có thể rộng. "
                    : "") +
                "Độ vừa vặn còn tùy phom và chất liệu; đây là gợi ý tham khảo."
        );
    }

    public static String label(String key) {
        return switch (key) {
            case "bustCm" -> "vòng ngực";
            case "waistCm" -> "vòng eo";
            case "hipCm" -> "vòng ba";
            case "shoulderCm" -> "vai";
            default -> key;
        };
    }
}
