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
  private final com.learnflow.backend.store.StoreInfoService store;
  private final com.learnflow.backend.store.SizeGuideService sizes;

  public FashionAIService(
    FashionAIProvider p,
    CatalogService c,
    OrderService o,
    com.learnflow.backend.store.StoreInfoService store,
    com.learnflow.backend.store.SizeGuideService sizes
  ) {
    provider = p;
    catalog = c;
    orders = o;
    this.store = store;
    this.sizes = sizes;
  }

  public AiStatusResponse status() {
    return provider.status();
  }

  public ChatResponse chat(Long user, ChatRequest r) {
    Long orderId = r.orderId();
    if (orderId == null) {
      var matcher = java.util.regex.Pattern.compile(
        "(?:đơn(?: hàng)?|order|mã đơn)\\s*[#:]?\\s*(\\d+)",
        java.util.regex.Pattern.CASE_INSENSITIVE |
          java.util.regex.Pattern.UNICODE_CASE
      ).matcher(r.message());
      if (matcher.find()) try {
        orderId = Long.valueOf(matcher.group(1));
      } catch (NumberFormatException ignored) {}
    }
    if (orderId != null) {
      var order = orders.get(user, orderId, false);
      return new ChatResponse(
        "Tra cứu đơn hàng",
        "Đơn hàng #" +
          order.orderId() +
          ": " +
          statusLabel(order.orderStatus()) +
          ". Thanh toán: " +
          statusLabel(order.payment().paymentStatus()) +
          ". " +
          store.contactText(),
        null,
        List.of()
      );
    }
    var faq = store.answer(r.message());
    if (faq.isPresent()) return new ChatResponse(
      "Chính sách cửa hàng",
      faq.get(),
      null,
      List.of()
    );
    String text = r.message().toLowerCase(Locale.ROOT);
    boolean sizeRequest =
      java.util.regex.Pattern.compile("(?iu)(?<![\\p{L}])size(?![\\p{L}])")
        .matcher(text)
        .find() ||
      text.contains("kích cỡ") ||
      r.bustCm() != null ||
      r.waistCm() != null ||
      r.hipCm() != null ||
      r.shoulderCm() != null;
    if (sizeRequest) {
      com.learnflow.backend.catalog.dto.ProductResponse product =
        r.productId() == null ? null : catalog.get(r.productId());
      String code =
        product == null ? r.sizeChartCode() : product.sizeChartCode();
      if (code == null) return new ChatResponse(
        "Bảng size cửa hàng",
        "Vui lòng chọn bảng size hoặc sản phẩm đã gắn bảng size. Cửa hàng chưa có bảng riêng cho sản phẩm này; không thể suy ra size chỉ từ chiều cao/cân nặng.",
        null,
        List.of()
      );
      Map<String, java.math.BigDecimal> measures = new HashMap<>();
      measures.put("bustCm", r.bustCm());
      measures.put("waistCm", r.waistCm());
      measures.put("hipCm", r.hipCm());
      measures.put("shoulderCm", r.shoulderCm());
      Set<String> stock =
        product == null
          ? null
          : product
              .variants()
              .stream()
              .filter(v -> v.stockQuantity() > 0)
              .map(
                com.learnflow.backend.catalog.dto.ProductResponse.VariantResponse::size
              )
              .collect(java.util.stream.Collectors.toSet());
      var advice = sizes.advise(code, measures, stock);
      return new ChatResponse(
        "Bảng size cửa hàng",
        advice.explanation(),
        advice.size(),
        product == null ? List.of() : List.of(product),
        advice
      );
    }
    if (!r.allowExternalAi() || !provider.configured()) return new ChatResponse(
      "Gợi ý theo dữ liệu cửa hàng",
      "Bạn có thể tham khảo các sản phẩm dưới đây. Bật tùy chọn dùng Gemini để phân tích câu hỏi theo ngữ cảnh; bảng size và chính sách vẫn dùng được khi chưa có API key.",
      null,
      catalog.contextSuggestions(r.message())
    );
    var maximum = catalog.maximumPrice(r.message());
    var candidates = catalog
      .availableCatalog()
      .stream()
      .filter(
        p ->
          maximum == null ||
          (p.salePrice() == null ? p.price() : p.salePrice()).compareTo(
            maximum
          ) <= 0
      )
      .toList();
    try {
      var result = provider.advise(r, candidates);
      if (result == null) throw new IllegalStateException();
      Map<Long, com.learnflow.backend.catalog.dto.ProductResponse> verified =
        new LinkedHashMap<>();
      for (Long id : result.productIds() == null
        ? List.<Long>of()
        : result.productIds()) {
        if (
          id == null ||
          candidates.stream().noneMatch(p -> p.productId().equals(id))
        ) continue;
        try {
          var p = catalog.get(id);
          if (!"Active".equals(p.status())) continue;
          if (
            p
              .variants()
              .stream()
              .noneMatch(v -> v.stockQuantity() > 0)
          ) continue;
          var effectivePrice =
            p.salePrice() == null ? p.price() : p.salePrice();
          if (
            (maximum != null && effectivePrice.compareTo(maximum) > 0) ||
            (result.maxPrice() != null &&
              effectivePrice.compareTo(result.maxPrice()) > 0)
          ) continue;
          verified.put(id, p);
        } catch (com.learnflow.backend.common.web.ApiException ignored) {}
        if (verified.size() >= 8) break;
      }
      var products = List.copyOf(verified.values());
      boolean allProductsVerified =
        result.productIds() != null &&
        new HashSet<>(result.productIds()).equals(verified.keySet());
      String context = switch (Objects.toString(result.occasion(), "")) {
        case "work" -> "đi làm";
        case "wedding" -> "dự tiệc cưới";
        case "summer" -> "mùa hè";
        case "casual" -> "đi chơi hằng ngày";
        default -> "nhu cầu của bạn";
      };
      String answer = products.isEmpty()
        ? "Chưa tìm thấy sản phẩm còn hàng đáp ứng yêu cầu trong danh mục hiện tại. Bạn có thể đổi ngân sách hoặc mô tả phong cách khác."
        : "Gợi ý cho " +
          context +
          ": " +
          products
            .stream()
            .map(com.learnflow.backend.catalog.dto.ProductResponse::productName)
            .collect(java.util.stream.Collectors.joining(", ")) +
          ". Giá và tồn kho đã được đối chiếu với cửa hàng. Bạn có thể chọn sản phẩm để xem màu, size và tư vấn số đo.";
      if (
        "Gemini API".equals(result.mode()) &&
        result.answer() != null &&
        !result.answer().isBlank() &&
        allProductsVerified
      ) answer = result.answer();
      return new ChatResponse(
        Set.of("Gemini API", "Gemini + RAG").contains(
          Objects.toString(result.mode(), "")
        )
          ? result.mode()
          : "Gợi ý local",
        answer,
        null,
        products
      );
    } catch (AIProviderException ex) {
      return new ChatResponse(
        "Gợi ý local",
        ex.getMessage() + " Đây là gợi ý local theo dữ liệu cửa hàng.",
        null,
        catalog.contextSuggestions(r.message())
      );
    } catch (Exception ex) {
      return new ChatResponse(
        "Gợi ý local",
        "Gemini hiện chưa thể phản hồi. Đây là gợi ý theo dữ liệu cửa hàng; bạn có thể thử lại sau.",
        null,
        catalog.contextSuggestions(r.message())
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
