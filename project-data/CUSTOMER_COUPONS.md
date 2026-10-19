# Hiển thị mã giảm giá cho khách hàng

- `GET /api/coupons` cho phép khách chưa đăng nhập xem các mã có hiệu lực tại thời điểm hiện tại và còn lượt dùng. API quản trị vẫn yêu cầu quyền Admin.
- Trang chủ hiển thị mã, mức giảm, giá trị đơn tối thiểu, thời hạn và nút sao chép; không còn quảng cáo cố định WELCOME10.
- Thanh toán cho phép chọn mã từ danh sách hoặc nhập thủ công. Mã chưa đủ giá trị đơn được vô hiệu hóa kèm số tiền cần mua thêm.
- Danh sách làm mới mỗi 30 giây và khi quay lại cửa sổ. Backend kiểm tra lại thời hạn, lượt dùng và đơn tối thiểu khi báo giá/đặt hàng.
- Quy tắc đếm lượt giữ nguyên hệ thống hiện có: các đơn đã ghi nhận mã đều được tính, kể cả đơn chờ hoặc hủy.
- Lỗi tải danh sách có nút thử lại; danh sách rỗng không hiển thị mã thay thế giả định.
- Không cần migration hoặc thay đổi biến môi trường.

## Cập nhật EC2

1. Merge PR vào `dev`, sau đó phát hành `dev` sang `main` theo quy trình dự án.
2. Chờ workflow backend thành công; kiểm tra `http://56.10.115.197/api/coupons` trả JSON.
3. Build lại frontend từ bản mã đã merge, chuyển `dist` lên một release mới và đổi symlink `/var/www/forme/current` như hướng dẫn triển khai Nginx hiện tại. Push GitHub chưa tự cập nhật frontend vì chưa có workflow frontend.
4. Kiểm tra trang chủ và thanh toán; thay đổi mã trong Admin và kiểm tra danh sách khách hàng sau khi làm mới.

Trên HTTP, trình duyệt có thể chặn Clipboard API. Khi đó thông báo vẫn hiển thị mã để người dùng sao chép thủ công; HTTPS hỗ trợ đầy đủ thao tác sao chép.

## Kiểm chứng

- Backend: Maven `verify` trên MySQL local database `fashion_store_test` và Redis local; 50 bài đạt, không bỏ qua.
- Frontend: lint và build đạt; 36 bài đạt khi chạy Vitest với một worker. Lần chạy song song đầu tiên có một test quản trị cũ vượt timeout; chạy lại toàn bộ với một worker đạt, không tăng timeout hay bỏ qua test.
- Kiểm thử tích hợp xác nhận API công khai loại mã chưa bắt đầu, hết hạn đúng thời điểm kết thúc, giới hạn 0 và hết lượt do đơn đã đặt; API quản trị không bị mở công khai.
- Kiểm thử giao diện xác nhận sao chép mã, ẩn mã hết hạn, điều kiện đơn tối thiểu, áp dụng mã và trạng thái trống/lỗi/thử lại.
