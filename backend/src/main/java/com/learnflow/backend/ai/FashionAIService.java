package com.learnflow.backend.ai;

import com.learnflow.backend.ai.dto.*;
import com.learnflow.backend.catalog.CatalogService;
import com.learnflow.backend.order.OrderService;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class FashionAIService {

    private final FashionAIProvider provider;
    private final CatalogService catalog;
    private final OrderService orders;

    public FashionAIService(FashionAIProvider p, CatalogService c, OrderService o) {
        provider = p;
        catalog = c;
        orders = o;
    }

    public AiStatusResponse status() {
        return new AiStatusResponse(provider.configured(), provider.configured() ? "API" : "Chưa kết nối");
    }

    public ChatResponse chat(Long user, ChatRequest r) {
        if (r.orderId() != null) {
            var order = orders.get(user, r.orderId(), false);
            return new ChatResponse(
                "Hỗ trợ cửa hàng",
                "Đơn hàng #" +
                    order.orderId() +
                    ": " +
                    statusLabel(order.orderStatus()) +
                    ". Thanh toán: " +
                    statusLabel(order.payment().paymentStatus()) +
                    ".",
                null,
                List.of()
            );
        }
        String text = r.message().toLowerCase(Locale.ROOT);
        if (
            text.contains("shipping") ||
            text.contains("phí ship") ||
            text.contains("vận chuyển") ||
            text.contains("giao hàng")
        ) return new ChatResponse(
            "Chính sách cửa hàng",
            "Phí vận chuyển là 30.000₫. Đơn hàng từ 1.000.000₫ được miễn phí vận chuyển.",
            null,
            List.of()
        );
        if (
            text.contains("return") ||
            text.contains("đổi trả") ||
            text.contains("trả hàng") ||
            text.contains("đổi hàng")
        ) return new ChatResponse(
            "Chính sách cửa hàng",
            "Vui lòng liên hệ cửa hàng để yêu cầu đổi trả trong vòng 7 ngày kể từ khi nhận hàng. Sản phẩm cần chưa qua sử dụng và còn nguyên tem mác. Cửa hàng sẽ xét duyệt yêu cầu đổi trả.",
            null,
            List.of()
        );
        var candidates = catalog.list("", null, null, 0, 100, false).content();
        if (!provider.configured()) return new ChatResponse(
            "Chưa kết nối",
            "Trợ lý AI chưa được kết nối. Bạn có thể xem gợi ý từ cửa hàng bên dưới, hỏi về vận chuyển hoặc nhập mã đơn hàng của mình để tra cứu.",
            null,
            catalog.suggestions(user, null, false)
        );
        try {
            var result = provider.advise(r, candidates);
            if (result == null) throw new IllegalStateException();
            Set<Long> ids = result.productIds() == null ? Set.of() : new HashSet<>(result.productIds());
            var verified = candidates
                .stream()
                .filter(p -> ids.contains(p.productId()))
                .limit(8)
                .toList();
            String size = Set.of("XS", "S", "M", "L", "XL", "XXL").contains(
                Objects.toString(result.suggestedSize(), "")
            )
                ? result.suggestedSize()
                : null;
            return new ChatResponse(
                "AI API",
                verified.isEmpty()
                    ? "Chưa tìm thấy sản phẩm phù hợp trong danh mục hiện tại của cửa hàng."
                    : "Đây là những sản phẩm của cửa hàng phù hợp với yêu cầu của bạn. Hãy kiểm tra thông số sản phẩm trước khi chọn kích cỡ.",
                size,
                verified
            );
        } catch (Exception ex) {
            return new ChatResponse(
                "Tạm thời không khả dụng",
                "Dịch vụ AI hiện chưa thể phản hồi. Bạn vẫn có thể mua sắm và sử dụng chức năng hỗ trợ cửa hàng.",
                null,
                List.of()
            );
        }
    }

    private String statusLabel(String value) {
        return switch (value) {
            case "Pending" -> "Chờ xác nhận";
            case "Confirmed" -> "Đã xác nhận";
            case "Shipping" -> "Đang giao hàng";
            case "Delivered" -> "Đã giao hàng";
            case "Cancelled" -> "Đã hủy";
            case "Paid" -> "Đã thanh toán";
            case "Failed" -> "Không thành công";
            case "Refunded" -> "Đã ghi nhận hoàn tiền";
            default -> value;
        };
    }
}
