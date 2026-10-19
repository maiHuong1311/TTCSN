package com.learnflow.backend.common.web;

import java.util.Map;

public final class VietnameseMessages {

    private VietnameseMessages() {}

    private static final Map<String, String> MESSAGES = Map.ofEntries(
        Map.entry("Email is already registered", "Địa chỉ email này đã được đăng ký."),
        Map.entry("Username is already registered", "Tên đăng nhập này đã được sử dụng."),
        Map.entry("Invalid email or password", "Email, tên đăng nhập hoặc mật khẩu không chính xác."),
        Map.entry("Account is blocked", "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên."),
        Map.entry("User no longer exists", "Tài khoản không còn tồn tại."),
        Map.entry("Cannot block your own account", "Bạn không thể tự khóa tài khoản của mình."),
        Map.entry("Invalid status", "Trạng thái không hợp lệ."),
        Map.entry("User not found", "Không tìm thấy người dùng."),
        Map.entry("Invalid or expired access token", "Phiên đăng nhập không hợp lệ hoặc đã hết hạn."),
        Map.entry("A valid Bearer token is required", "Vui lòng đăng nhập để tiếp tục."),
        Map.entry("Admin access is required", "Bạn cần quyền quản trị viên để thực hiện thao tác này."),
        Map.entry("Category cycle", "Danh mục cha không được tạo thành vòng lặp."),
        Map.entry("Invalid pagination", "Thông tin phân trang không hợp lệ."),
        Map.entry("Category does not exist", "Danh mục không tồn tại."),
        Map.entry("Sale price exceeds price", "Giá khuyến mãi không được lớn hơn giá gốc."),
        Map.entry("Variant belongs to another product", "Biến thể thuộc về một sản phẩm khác."),
        Map.entry("Duplicate variant", "Biến thể bị trùng lặp."),
        Map.entry(
            "Keep existing variants; set stock to zero to retire them",
            "Vui lòng giữ các biến thể hiện có; đặt tồn kho bằng 0 để ngừng bán."
        ),
        Map.entry(
            "Product unavailable or insufficient stock",
            "Sản phẩm ngừng bán hoặc không đủ số lượng tồn kho."
        ),
        Map.entry("Catalog item not found", "Không tìm thấy sản phẩm hoặc danh mục."),
        Map.entry("Address not found", "Không tìm thấy địa chỉ giao hàng."),
        Map.entry(
            "Insufficient stock or quantity exceeds 100",
            "Tồn kho không đủ hoặc số lượng vượt quá 100 sản phẩm."
        ),
        Map.entry("Cart item not found", "Không tìm thấy sản phẩm trong giỏ hàng."),
        Map.entry(
            "Invalid coupon dates or percentage",
            "Thời gian áp dụng hoặc tỷ lệ giảm giá không hợp lệ."
        ),
        Map.entry("Coupon not found", "Không tìm thấy mã giảm giá."),
        Map.entry("Coupon is not active", "Mã giảm giá chưa có hiệu lực hoặc đã hết hạn."),
        Map.entry("Coupon usage limit reached", "Mã giảm giá đã hết lượt sử dụng."),
        Map.entry("Coupon already used by customer", "Bạn đã sử dụng mã giảm giá này. Mỗi khách hàng chỉ được dùng mỗi mã một lần."),
        Map.entry(
            "Order does not meet coupon minimum",
            "Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã giảm giá."
        ),
        Map.entry("Cart is empty", "Giỏ hàng đang trống."),
        Map.entry(
            "Only pending orders can be cancelled by customers",
            "Bạn chỉ có thể hủy đơn hàng đang chờ xác nhận."
        ),
        Map.entry("Invalid order status transition", "Không thể chuyển đơn hàng sang trạng thái này."),
        Map.entry(
            "Online payment must be confirmed before shipping",
            "Cần xác nhận thanh toán trực tuyến trước khi giao hàng."
        ),
        Map.entry("Payment cannot be confirmed", "Không thể xác nhận khoản thanh toán này."),
        Map.entry("Transaction reference is required", "Vui lòng nhập mã giao dịch thanh toán."),
        Map.entry("Order not found", "Không tìm thấy đơn hàng."),
        Map.entry(
            "Reviews require a delivered purchase",
            "Bạn chỉ có thể đánh giá sản phẩm đã mua và được giao thành công."
        ),
        Map.entry("You already reviewed this product", "Bạn đã đánh giá sản phẩm này."),
        Map.entry("Request validation failed", "Thông tin chưa hợp lệ. Vui lòng kiểm tra lại."),
        Map.entry("Malformed request or invalid parameter", "Dữ liệu gửi lên hoặc tham số không hợp lệ."),
        Map.entry(
            "A unique value is already used or this record is referenced by other data",
            "Dữ liệu bị trùng hoặc đang được sử dụng bởi bản ghi khác."
        ),
        Map.entry("An unexpected server error occurred", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."),
        Map.entry("Failed to fetch", "Không thể kết nối với máy chủ. Vui lòng kiểm tra kết nối."),
        Map.entry(
            "NetworkError when attempting to fetch resource.",
            "Không thể kết nối với máy chủ. Vui lòng kiểm tra kết nối."
        ),
        Map.entry("Request failed", "Không thể thực hiện yêu cầu. Vui lòng thử lại."),
        Map.entry("Advisor unavailable", "Trợ lý hiện không khả dụng."),
        Map.entry(
            "Advisor connection ended before a response",
            "Kết nối với trợ lý đã kết thúc trước khi có phản hồi. Vui lòng thử lại."
        )
    );

    public static String translate(String message) {
        return message == null ? "Giá trị không hợp lệ." : MESSAGES.getOrDefault(message, message);
    }

    public static String title(int status) {
        return switch (status) {
            case 400 -> "Yêu cầu không hợp lệ";
            case 401 -> "Chưa xác thực";
            case 403 -> "Không có quyền truy cập";
            case 404 -> "Không tìm thấy";
            case 409 -> "Dữ liệu bị trùng hoặc đang được sử dụng";
            case 429 -> "Quá nhiều yêu cầu";
            case 503 -> "Dịch vụ chưa khả dụng";
            default -> "Lỗi hệ thống";
        };
    }

    public static String validation(String code) {
        if (code == null) return "Giá trị không hợp lệ.";
        return switch (code) {
            case "NotBlank", "NotEmpty", "NotNull" -> "Không được để trống.";
            case "Email" -> "Địa chỉ email không hợp lệ.";
            case "Size" -> "Độ dài hoặc số phần tử không nằm trong giới hạn cho phép.";
            case "Pattern" -> "Định dạng không hợp lệ.";
            case "Min", "DecimalMin" -> "Giá trị nhỏ hơn mức tối thiểu cho phép.";
            case "Max", "DecimalMax" -> "Giá trị vượt quá mức tối đa cho phép.";
            default -> "Giá trị không hợp lệ.";
        };
    }
}
