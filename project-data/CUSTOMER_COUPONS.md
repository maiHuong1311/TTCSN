# Hiển thị mã giảm giá cho khách hàng

- `GET /api/coupons` cho phép khách chưa đăng nhập xem các mã có hiệu lực tại thời điểm hiện tại và còn lượt dùng. API quản trị vẫn yêu cầu quyền Admin.
- Mục **MÃ GIẢM GIÁ** trên thanh điều hướng mở trang `/coupons`, cho phép xem danh sách, sao chép và làm mới để lấy các mã admin vừa thêm. Trang công khai; tài khoản Admin vẫn ở giao diện quản trị riêng.
- Trang chủ hiển thị mã, mức giảm, giá trị đơn tối thiểu, thời hạn và nút sao chép; không còn quảng cáo cố định WELCOME10.
- Thanh toán cho phép chọn mã từ danh sách hoặc nhập thủ công. Mã chưa đủ giá trị đơn được vô hiệu hóa kèm số tiền cần mua thêm.
- Danh sách làm mới mỗi 30 giây và khi quay lại cửa sổ. Backend kiểm tra lại thời hạn, lượt dùng và đơn tối thiểu khi báo giá/đặt hàng.
- Quy tắc đếm lượt giữ nguyên hệ thống hiện có: các đơn đã ghi nhận mã đều được tính, kể cả đơn chờ hoặc hủy.
- Mỗi khách hàng chỉ dùng mỗi mã một lần. Lượt được ghi nhận khi tạo đơn thành công; báo giá hoặc đặt hàng thất bại không tiêu hao lượt. Hủy đơn không hoàn lại lượt dùng mã.
- Khách đã đăng nhập chỉ thấy các mã mình chưa dùng; khách chưa đăng nhập vẫn thấy các mã còn hiệu lực chung. Cache frontend tách theo tài khoản để không lẫn danh sách khi đổi người dùng.
- Backend kiểm tra lịch sử theo cặp khách hàng/mã ở cả báo giá và tạo đơn. Tạo đơn giữ khóa khách hàng đến khi commit, kết hợp khóa mã hiện có để ngăn hai đơn đồng thời vượt giới hạn.
- Lỗi tải danh sách có nút thử lại; danh sách rỗng không hiển thị mã thay thế giả định.
- Không cần migration hoặc thay đổi biến môi trường.

## Cập nhật EC2

1. Merge PR vào `dev`, sau đó phát hành `dev` sang `main` theo quy trình dự án.
2. Chờ workflow backend thành công; kiểm tra `http://56.10.115.197/api/coupons` trả JSON.
3. Build lại frontend từ bản mã đã merge, chuyển `dist` lên một release mới và đổi symlink `/var/www/forme/current` như hướng dẫn triển khai Nginx hiện tại. Push GitHub chưa tự cập nhật frontend vì chưa có workflow frontend.
4. Kiểm tra trang chủ và thanh toán; thay đổi mã trong Admin và kiểm tra danh sách khách hàng sau khi làm mới.

Trên HTTP, trình duyệt có thể chặn Clipboard API. Khi đó thông báo vẫn hiển thị mã để người dùng sao chép thủ công; HTTPS hỗ trợ đầy đủ thao tác sao chép.

## Kiểm chứng

- Backend: Maven `verify` trên MySQL local database `fashion_store_test` và Redis local; 51 bài đạt, không bỏ qua.
- Frontend: lint và build đạt; 39 bài đạt khi chạy Vitest với một worker.
- Kiểm thử tích hợp xác nhận API công khai loại mã chưa bắt đầu, hết hạn đúng thời điểm kết thúc, giới hạn 0 và hết lượt do đơn đã đặt; API quản trị không bị mở công khai.
- Kiểm thử giao diện xác nhận sao chép mã, ẩn mã hết hạn, điều kiện đơn tối thiểu, áp dụng mã và trạng thái trống/lỗi/thử lại.

- Kiểm thử bổ sung: báo giá lặp không mất lượt; báo giá/đặt hàng với mã đã dùng bị chặn kể cả sau khi hủy đơn; giỏ hàng giữ nguyên; khách khác vẫn được dùng lần đầu; cache làm mới khi đổi tài khoản.

## Chẩn đoán mã không xuất hiện

- Kiểm tra `GET /api/coupons`: nếu trả mã nhưng giao diện không hiển thị, cần cập nhật bản build frontend trên Nginx. Chỉ cập nhật backend bằng GitHub Actions không thay đổi frontend.
- Trang `/coupons` có nút **Làm mới danh sách**; danh sách cũng tự tải mỗi 30 giây và khi quay lại cửa sổ.
- Nếu API không trả mã: kiểm tra thời gian bắt đầu/kết thúc, lượt sử dụng chung và lịch sử dùng mã của tài khoản hiện tại. Đơn chưa đạt tối thiểu vẫn có thể xem mã; điều kiện được kiểm tra khi thanh toán.

- Trang mã riêng: kiểm thử truy cập trực tiếp `/coupons`, điều hướng từ menu, hiển thị mã API và làm mới sau khi admin thêm mã.

## Kiểm tra triển khai trang riêng

Frontend đã được cập nhật lên EC2 ngày 20/10/2026 bằng release riêng, giữ lại bản cũ. HTTP `/coupons` và file JavaScript mới trả 200; API trả SUMMERSALES và WELCOME10, backend health UP. Không thay đổi database hoặc cấu hình backend.
